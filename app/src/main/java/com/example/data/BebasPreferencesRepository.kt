package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.bebasDataStore: DataStore<Preferences> by preferencesDataStore(name = "bebas_preferences")

data class BookmarkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class RecentSession(
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis(),
)

data class BebasPreferencesState(
    val keepScreenOn: Boolean = true,
    val backgroundKeepAlive: Boolean = true,
    val sessionGuardEnabled: Boolean = true,
    val forceCopyEnabled: Boolean = true,
    val accidentalExitLock: Boolean = true,
    val desktopMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val aiBaseUrl: String = DEFAULT_AI_BASE_URL,
    val aiModel: String = DEFAULT_AI_MODEL,
    val bookmarks: List<BookmarkItem> = emptyList(),
    val recentSessions: List<RecentSession> = emptyList(),
) {
    companion object {
        const val DEFAULT_AI_BASE_URL = "https://api.xingchiapi.com/v1"
        const val DEFAULT_AI_MODEL = "gpt-6.1-sol"
    }
}

class BebasPreferencesRepository(private val context: Context) {

    private object Keys {
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val BACKGROUND_KEEP_ALIVE = booleanPreferencesKey("background_keep_alive")
        val SESSION_GUARD = booleanPreferencesKey("session_guard_enabled")
        val FORCE_COPY = booleanPreferencesKey("force_copy_enabled")
        val ACCIDENTAL_EXIT_LOCK = booleanPreferencesKey("accidental_exit_lock")
        val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val BOOKMARKS_JSON = stringPreferencesKey("bookmarks_json")
        val RECENT_SESSIONS_JSON = stringPreferencesKey("recent_sessions_json")
    }

    val preferencesFlow: Flow<BebasPreferencesState> = context.bebasDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val themeMode = ThemeMode.entries.firstOrNull { it.name == themeName } ?: ThemeMode.DARK

        BebasPreferencesState(
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            backgroundKeepAlive = prefs[Keys.BACKGROUND_KEEP_ALIVE] ?: true,
            sessionGuardEnabled = prefs[Keys.SESSION_GUARD] ?: true,
            forceCopyEnabled = prefs[Keys.FORCE_COPY] ?: true,
            accidentalExitLock = prefs[Keys.ACCIDENTAL_EXIT_LOCK] ?: true,
            desktopMode = prefs[Keys.DESKTOP_MODE] ?: false,
            themeMode = themeMode,
            aiBaseUrl = prefs[Keys.AI_BASE_URL] ?: BebasPreferencesState.DEFAULT_AI_BASE_URL,
            aiModel = prefs[Keys.AI_MODEL] ?: BebasPreferencesState.DEFAULT_AI_MODEL,
            bookmarks = parseBookmarks(prefs[Keys.BOOKMARKS_JSON]),
            recentSessions = parseRecentSessions(prefs[Keys.RECENT_SESSIONS_JSON]),
        )
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.bebasDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setBackgroundKeepAlive(enabled: Boolean) {
        context.bebasDataStore.edit { it[Keys.BACKGROUND_KEEP_ALIVE] = enabled }
    }

    suspend fun setSessionGuardEnabled(enabled: Boolean) {
        context.bebasDataStore.edit { it[Keys.SESSION_GUARD] = enabled }
    }

    suspend fun setForceCopyEnabled(enabled: Boolean) {
        context.bebasDataStore.edit { it[Keys.FORCE_COPY] = enabled }
    }

    suspend fun setAccidentalExitLock(enabled: Boolean) {
        context.bebasDataStore.edit { it[Keys.ACCIDENTAL_EXIT_LOCK] = enabled }
    }

    suspend fun setDesktopMode(enabled: Boolean) {
        context.bebasDataStore.edit { it[Keys.DESKTOP_MODE] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.bebasDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAiConfig(baseUrl: String, model: String) {
        val cleanUrl = baseUrl.trim().trimEnd('/').ifEmpty {
            BebasPreferencesState.DEFAULT_AI_BASE_URL
        }
        val cleanModel = model.trim().ifEmpty {
            BebasPreferencesState.DEFAULT_AI_MODEL
        }
        context.bebasDataStore.edit {
            it[Keys.AI_BASE_URL] = cleanUrl
            it[Keys.AI_MODEL] = cleanModel
        }
    }

    suspend fun addOrUpdateBookmark(title: String, url: String) {
        val cleanTitle = title.trim().ifEmpty { url.trim() }
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return

        context.bebasDataStore.edit { prefs ->
            val current = parseBookmarks(prefs[Keys.BOOKMARKS_JSON]).toMutableList()
            val existingIndex = current.indexOfFirst { it.url.equals(cleanUrl, ignoreCase = true) }
            if (existingIndex >= 0) {
                current[existingIndex] = current[existingIndex].copy(title = cleanTitle)
            } else {
                current.add(0, BookmarkItem(title = cleanTitle, url = cleanUrl))
            }
            prefs[Keys.BOOKMARKS_JSON] = serializeBookmarks(current)
        }
    }

    suspend fun removeBookmark(url: String) {
        context.bebasDataStore.edit { prefs ->
            val updated = parseBookmarks(prefs[Keys.BOOKMARKS_JSON])
                .filterNot { it.url.equals(url, ignoreCase = true) }
            prefs[Keys.BOOKMARKS_JSON] = serializeBookmarks(updated)
        }
    }

    suspend fun recordRecentSession(url: String, title: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty() || cleanUrl == "about:blank") return
        val cleanTitle = title.trim().ifEmpty { cleanUrl }

        context.bebasDataStore.edit { prefs ->
            val current = parseRecentSessions(prefs[Keys.RECENT_SESSIONS_JSON]).toMutableList()
            current.removeAll { it.url.equals(cleanUrl, ignoreCase = true) }
            current.add(0, RecentSession(url = cleanUrl, title = cleanTitle))
            val trimmed = current.take(8)
            prefs[Keys.RECENT_SESSIONS_JSON] = serializeRecentSessions(trimmed)
        }
    }

    suspend fun removeRecentSession(url: String) {
        context.bebasDataStore.edit { prefs ->
            val updated = parseRecentSessions(prefs[Keys.RECENT_SESSIONS_JSON])
                .filterNot { it.url.equals(url, ignoreCase = true) }
            prefs[Keys.RECENT_SESSIONS_JSON] = serializeRecentSessions(updated)
        }
    }

    suspend fun clearRecentSessions() {
        context.bebasDataStore.edit { prefs ->
            prefs.remove(Keys.RECENT_SESSIONS_JSON)
        }
    }

    private fun parseBookmarks(raw: String?): List<BookmarkItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        BookmarkItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            url = obj.optString("url", ""),
                            createdAt = obj.optLong("createdAt", 0L),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeBookmarks(items: List<BookmarkItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("url", item.url)
                    put("createdAt", item.createdAt)
                },
            )
        }
        return array.toString()
    }

    private fun parseRecentSessions(raw: String?): List<RecentSession> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        RecentSession(
                            url = obj.optString("url", ""),
                            title = obj.optString("title", ""),
                            visitedAt = obj.optLong("visitedAt", 0L),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeRecentSessions(items: List<RecentSession>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("url", item.url)
                    put("title", item.title)
                    put("visitedAt", item.visitedAt)
                },
            )
        }
        return array.toString()
    }
}
