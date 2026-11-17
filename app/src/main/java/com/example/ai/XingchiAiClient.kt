package com.example.ai

import com.example.security.SecureKeyVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val modelUsed: String? = null,
    val isError: Boolean = false,
) {
    enum class Role { USER, ASSISTANT }
}

sealed interface AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>
    data class Error(val message: String, val errorCode: String? = null) : AiResult<Nothing>
}

class XingchiAiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun resolveApiKey(): String = SecureKeyVault.resolveXingchiApiKey()

    suspend fun fetchModels(baseUrl: String): AiResult<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext AiResult.Error(
                message = "XINGCHI_API_KEY belum diatur di panel Secrets AI Studio.",
                errorCode = "missing_api_key",
            )
        }
        val cleanBase = baseUrl.trim().trimEnd('/')
        val request = Request.Builder()
            .url("$cleanBase/models")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()

        return@withContext try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use parseApiError(rawBody, response.code)
                }
                val json = JSONObject(rawBody)
                val data = json.optJSONArray("data") ?: JSONArray()
                val models = buildList {
                    for (i in 0 until data.length()) {
                        val id = data.optJSONObject(i)?.optString("id").orEmpty()
                        if (id.isNotBlank()) add(id)
                    }
                }
                AiResult.Success(models)
            }
        } catch (e: Exception) {
            AiResult.Error("Gagal mengambil daftar model: ${e.localizedMessage ?: "Kesalahan jaringan"}")
        }
    }

    /**
     * Sends a conversation to the configured OpenAI-compatible endpoint.
     * Tries the Responses API (/v1/responses) first (matching XingchiAPI docs),
     * and falls back to /v1/chat/completions if a custom endpoint returns 404/405.
     */
    suspend fun sendChat(
        baseUrl: String,
        model: String,
        history: List<AiChatMessage>,
        userMessage: String,
        pageContextTitle: String = "",
    ): AiResult<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext AiResult.Error(
                message = "API key AI belum dikonfigurasi. Tambahkan XINGCHI_API_KEY di panel Secrets AI Studio.",
                errorCode = "missing_api_key",
            )
        }

        val cleanBase = baseUrl.trim().trimEnd('/')
        val systemInstruction = buildString {
            append("Kamu adalah asisten AI cerdas di dalam aplikasi Android StayFlow. ")
            append("Bantu pengguna memahami dan menjawab soal, pertanyaan kuis, atau wawancara kerja dengan jelas, terstruktur, dan ringkas dalam bahasa Indonesia (atau ikuti bahasa pertanyaan). ")
            if (pageContextTitle.isNotBlank()) {
                append("Konteks halaman web yang sedang dibuka pengguna: \"$pageContextTitle\".")
            }
        }

        val responsesResult = callResponsesApi(
            baseUrl = cleanBase,
            apiKey = apiKey,
            model = model,
            systemInstruction = systemInstruction,
            history = history,
            userMessage = userMessage,
        )

        if (responsesResult is AiResult.Error && responsesResult.errorCode == "not_found_fallback") {
            return@withContext callChatCompletionsApi(
                baseUrl = cleanBase,
                apiKey = apiKey,
                model = model,
                systemInstruction = systemInstruction,
                history = history,
                userMessage = userMessage,
            )
        }

        responsesResult
    }

    private fun callResponsesApi(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemInstruction: String,
        history: List<AiChatMessage>,
        userMessage: String,
    ): AiResult<String> {
        val inputItems = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                },
            )
            history.filter { !it.isError }.takeLast(8).forEach { msg ->
                put(
                    JSONObject().apply {
                        put("role", if (msg.role == AiChatMessage.Role.USER) "user" else "assistant")
                        put("content", msg.content)
                    },
                )
            }
            put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                },
            )
        }

        val payload = JSONObject().apply {
            put("model", model)
            put("input", inputItems)
        }

        val request = Request.Builder()
            .url("$baseUrl/responses")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .header("x-openai-actor-authorization", "local-image-extension")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (response.code == 404 || response.code == 405) {
                    return@use AiResult.Error("Fallback to chat completions", "not_found_fallback")
                }
                if (!response.isSuccessful) {
                    return@use parseApiError(rawBody, response.code)
                }
                val text = extractResponsesOutputText(rawBody)
                if (text.isNotBlank()) {
                    AiResult.Success(text)
                } else {
                    AiResult.Error("Respons AI kosong dari server.")
                }
            }
        } catch (e: Exception) {
            AiResult.Error("Koneksi AI gagal: ${e.localizedMessage ?: "Periksa internet Anda"}")
        }
    }

    private fun callChatCompletionsApi(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemInstruction: String,
        history: List<AiChatMessage>,
        userMessage: String,
    ): AiResult<String> {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemInstruction))
            history.filter { !it.isError }.takeLast(8).forEach { msg ->
                put(
                    JSONObject()
                        .put("role", if (msg.role == AiChatMessage.Role.USER) "user" else "assistant")
                        .put("content", msg.content),
                )
            }
            put(JSONObject().put("role", "user").put("content", userMessage))
        }

        val payload = JSONObject().apply {
            put("model", model)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use parseApiError(rawBody, response.code)
                }
                val text = extractChatCompletionsText(rawBody)
                if (text.isNotBlank()) {
                    AiResult.Success(text)
                } else {
                    AiResult.Error("Respons AI kosong dari server.")
                }
            }
        } catch (e: Exception) {
            AiResult.Error("Koneksi AI gagal: ${e.localizedMessage ?: "Periksa internet Anda"}")
        }
    }

    internal fun extractResponsesOutputText(rawJson: String): String {
        return try {
            val json = JSONObject(rawJson)
            val direct = json.optString("output_text").trim()
            if (direct.isNotEmpty()) return direct

            val outputArr = json.optJSONArray("output")
            if (outputArr != null) {
                val builder = StringBuilder()
                for (i in 0 until outputArr.length()) {
                    val item = outputArr.optJSONObject(i) ?: continue
                    val contentArr = item.optJSONArray("content") ?: continue
                    for (j in 0 until contentArr.length()) {
                        val part = contentArr.optJSONObject(j) ?: continue
                        val partText = part.optString("text")
                        if (partText.isNotBlank()) {
                            if (builder.isNotEmpty()) builder.append("\n")
                            builder.append(partText)
                        }
                    }
                }
                if (builder.isNotEmpty()) return builder.toString().trim()
            }
            extractChatCompletionsText(rawJson)
        } catch (_: Exception) {
            ""
        }
    }

    internal fun extractChatCompletionsText(rawJson: String): String {
        return try {
            val json = JSONObject(rawJson)
            json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                .orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseApiError(rawBody: String, httpCode: Int): AiResult.Error {
        return try {
            val errorObj = JSONObject(rawBody).optJSONObject("error")
            val code = errorObj?.optString("code").orEmpty()
            val serverMessage = errorObj?.optString("message").orEmpty()
            val friendlyMessage = when (code) {
                "client_not_allowed" ->
                    "Server menolak koneksi langsung dari aplikasi/SDK ($code: $serverMessage). Atur Base URL yang mendukung aplikasi Android di tab Pengaturan -> Endpoint AI."
                else ->
                    serverMessage.ifBlank { "Server AI mengembalikan error HTTP $httpCode." }
            }
            AiResult.Error(message = friendlyMessage, errorCode = code.ifBlank { "http_$httpCode" })
        } catch (_: Exception) {
            AiResult.Error(
                message = "Server AI mengembalikan error HTTP $httpCode.",
                errorCode = "http_$httpCode",
            )
        }
    }
}
