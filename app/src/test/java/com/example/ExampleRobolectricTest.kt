package com.example

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.example.ai.XingchiAiClient
import com.example.browser.KeepAliveWebView
import com.example.browser.SessionGuardScript
import com.example.security.SecureKeyVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name and developer credit match StayFlow by Sky`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("StayFlow", context.getString(R.string.app_name))
        assertEquals("Developer: Sky", context.getString(R.string.developer_credit))
    }

    @Test
    fun `normalizeTargetUrl handles full urls, bare domains, and search queries`() {
        assertEquals(
            "https://example.com/quiz",
            SessionGuardScript.normalizeTargetUrl("https://example.com/quiz"),
        )
        assertEquals(
            "http://localhost:8080",
            SessionGuardScript.normalizeTargetUrl("http://localhost:8080"),
        )
        assertEquals(
            "https://forms.gle/abc123",
            SessionGuardScript.normalizeTargetUrl("forms.gle/abc123"),
        )
        assertTrue(
            SessionGuardScript.normalizeTargetUrl("soal interview android")
                .startsWith("https://www.google.com/search?q="),
        )
    }

    @Test
    fun `extractHostDisplay strips www prefix cleanly`() {
        assertEquals(
            "portal.kampus.ac.id",
            SessionGuardScript.extractHostDisplay("https://www.portal.kampus.ac.id/ujian/1"),
        )
    }

    @Test
    fun `KeepAliveWebView keeps window visibility and focus active across screen-off and app-switch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val webView = KeepAliveWebView(context)
        assertTrue(webView.isFocusable)
        assertTrue(webView.isFocusableInTouchMode)
        webView.keepAliveInBackground = true

        assertEquals(View.VISIBLE, webView.resolveEffectiveWindowVisibility(View.GONE))
        assertEquals(View.VISIBLE, webView.resolveEffectiveWindowVisibility(View.INVISIBLE))
        assertTrue(webView.resolveEffectiveWindowFocus(false))

        webView.keepAliveInBackground = false
        assertEquals(View.GONE, webView.resolveEffectiveWindowVisibility(View.GONE))
        assertFalse(webView.resolveEffectiveWindowFocus(false))
    }

    @Test
    fun `buildToggleJs switches focus guard and force copy flags dynamically`() {
        val toggleJs = SessionGuardScript.buildToggleJs(isActive = true, forceCopyEnabled = false)
        assertTrue(toggleJs.contains("window.__bebasGuardActive = true;"))
        assertTrue(toggleJs.contains("window.__stayFlowForceCopy = false;"))
        assertTrue(toggleJs.contains("__stayFlowApplyForceCopyCss(false);"))
    }

    @Test
    fun `XingchiAiClient parses Responses API and Chat Completions JSON payloads`() {
        val client = XingchiAiClient()

        val responsesDirect = """{"output_text":"Jawaban langsung dari Responses API"}"""
        assertEquals(
            "Jawaban langsung dari Responses API",
            client.extractResponsesOutputText(responsesDirect),
        )

        val responsesNested = """
            {
              "output": [
                {
                  "type": "message",
                  "content": [
                    {"type": "output_text", "text": "Poin pertama"},
                    {"type": "output_text", "text": "Poin kedua"}
                  ]
                }
              ]
            }
        """.trimIndent()
        assertEquals(
            "Poin pertama\nPoin kedua",
            client.extractResponsesOutputText(responsesNested),
        )

        val chatCompletions = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Ringkasan kelebihan dan kekurangan"
                  }
                }
              ]
            }
        """.trimIndent()
        assertEquals(
            "Ringkasan kelebihan dan kekurangan",
            client.extractChatCompletionsText(chatCompletions),
        )
    }

    @Test
    fun `SecureKeyVault unseals AES-256-GCM sharded secret and rejects tampered shards`() {
        val sampleSecret = "sample-vault-token-12345"
        val random = SecureRandom()
        val shardA = ByteArray(32).also { random.nextBytes(it) }
        val shardB = ByteArray(32).also { random.nextBytes(it) }
        val combined = ByteArray(32) { i -> (shardA[i].toInt() xor shardB[i].toInt()).toByte() }
        val aesKey = MessageDigest.getInstance("SHA-256").run {
            update(combined)
            update("StayFlow::Sky::KeyVault::v1".toByteArray(Charsets.UTF_8))
            digest()
        }
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val cipherBytes = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
            doFinal(sampleSecret.toByteArray(Charsets.UTF_8))
        }
        fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

        assertEquals(
            sampleSecret,
            SecureKeyVault.unsealKey(
                cipherHex = cipherBytes.hex(),
                ivHex = iv.hex(),
                shardAHex = shardA.hex(),
                shardBHex = shardB.hex(),
            ),
        )

        // Flip 1 byte in shardA -> GCM authentication tag must fail and return ""
        shardA[0] = (shardA[0].toInt() xor 0xFF).toByte()
        assertEquals(
            "",
            SecureKeyVault.unsealKey(
                cipherHex = cipherBytes.hex(),
                ivHex = iv.hex(),
                shardAHex = shardA.hex(),
                shardBHex = shardB.hex(),
            ),
        )
    }
}
