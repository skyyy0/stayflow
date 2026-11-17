package com.example.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiChatMessage
import com.example.ai.AiResult
import com.example.ai.XingchiAiClient
import com.example.browser.SessionGuardScript
import com.example.data.BebasPreferencesRepository
import com.example.data.BebasPreferencesState
import com.example.data.BookmarkItem
import com.example.data.RecentSession
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface BrowserScreenMode {
    data object Home : BrowserScreenMode
    data object ActiveWeb : BrowserScreenMode
}

enum class HomeTab(val title: String) {
    EXPLORE("Jelajah"),
    SAVED("Simpanan"),
    AI("Tanya AI"),
    SETTINGS("Perisai"),
}

data class BebasUiState(
    val screenMode: BrowserScreenMode = BrowserScreenMode.Home,
    val activeTab: HomeTab = HomeTab.EXPLORE,
    val urlInput: String = "",
    val currentUrl: String = "",
    val pageTitle: String = "",
    val loadingProgress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSslSecure: Boolean = false,
    val sessionSeconds: Long = 0L,
    val shieldedCount: Int = 0,
    val lastShieldedEvent: String? = null,
    val pendingExternalUrl: String? = null,
    val loadErrorMessage: String? = null,
    val aiDraftPrompt: String = "",
    val aiMessages: List<AiChatMessage> = emptyList(),
    val isAiLoading: Boolean = false,
    val availableAiModels: List<String> = listOf("gpt-6.1-sol", "gpt-6-astra"),
    val isFetchingModels: Boolean = false,
    val aiModelStatusMessage: String? = null,
    val preferences: BebasPreferencesState = BebasPreferencesState(),
)

class BebasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BebasPreferencesRepository(application.applicationContext)
    private val aiClient = XingchiAiClient()

    private val _uiState = MutableStateFlow(BebasUiState())
    val uiState: StateFlow<BebasUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartElapsedMs: Long = 0L
    private var lastShieldTimestamp: Long = 0L

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    fun selectHomeTab(tab: HomeTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateUrlInput(input: String) {
        _uiState.update { it.copy(urlInput = input) }
    }

    fun openUrl(rawInput: String): String {
        val target = SessionGuardScript.normalizeTargetUrl(rawInput)
        if (target.isEmpty()) return ""

        val wasInWeb = _uiState.value.screenMode == BrowserScreenMode.ActiveWeb
        if (!wasInWeb || sessionStartElapsedMs == 0L) {
            sessionStartElapsedMs = SystemClock.elapsedRealtime()
        }

        _uiState.update {
            it.copy(
                screenMode = BrowserScreenMode.ActiveWeb,
                urlInput = target,
                currentUrl = target,
                isLoading = true,
                loadingProgress = 10,
                loadErrorMessage = null,
                isSslSecure = target.startsWith("https://", ignoreCase = true),
                sessionSeconds = if (wasInWeb) computeElapsedSeconds() else 0L,
                shieldedCount = if (wasInWeb) it.shieldedCount else 0,
                lastShieldedEvent = if (wasInWeb) it.lastShieldedEvent else null,
            )
        }
        startSessionTimerIfNeeded()
        return target
    }

    fun returnHome() {
        stopSessionTimer()
        sessionStartElapsedMs = 0L
        _uiState.update {
            it.copy(
                screenMode = BrowserScreenMode.Home,
                isLoading = false,
                loadingProgress = 0,
                sessionSeconds = 0L,
                pendingExternalUrl = null,
                loadErrorMessage = null,
            )
        }
    }

    fun syncSessionClock() {
        if (_uiState.value.screenMode == BrowserScreenMode.ActiveWeb && sessionStartElapsedMs > 0L) {
            _uiState.update { it.copy(sessionSeconds = computeElapsedSeconds()) }
        }
    }

    fun onPageStarted(url: String) {
        _uiState.update {
            it.copy(
                currentUrl = url,
                urlInput = url,
                isLoading = true,
                loadErrorMessage = null,
                isSslSecure = url.startsWith("https://", ignoreCase = true),
            )
        }
    }

    fun onPageFinished(url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) {
        val resolvedTitle = title.trim().ifEmpty { SessionGuardScript.extractHostDisplay(url) }
        _uiState.update {
            it.copy(
                currentUrl = url,
                urlInput = url,
                pageTitle = resolvedTitle,
                isLoading = false,
                loadingProgress = 100,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isSslSecure = url.startsWith("https://", ignoreCase = true),
            )
        }
        viewModelScope.launch {
            repository.recordRecentSession(url = url, title = resolvedTitle)
        }
    }

    fun onProgressChanged(progress: Int, canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update {
            it.copy(
                loadingProgress = progress,
                isLoading = progress in 1..99,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
            )
        }
    }

    fun onPageTitleReceived(title: String) {
        if (title.isNotBlank()) {
            _uiState.update { it.copy(pageTitle = title.trim()) }
        }
    }

    fun onLoadError(description: String?) {
        _uiState.update {
            it.copy(
                isLoading = false,
                loadErrorMessage = description?.takeIf { msg -> msg.isNotBlank() }
                    ?: "Halaman gagal dimuat. Periksa koneksi internet atau alamat URL.",
            )
        }
    }

    fun clearLoadError() {
        _uiState.update { it.copy(loadErrorMessage = null) }
    }

    fun onInterruptionShielded(eventType: String) {
        val now = System.currentTimeMillis()
        val shouldIncrement = (now - lastShieldTimestamp) > 600L
        lastShieldTimestamp = now
        val friendlyLabel = when (eventType) {
            "visibilitychange", "webkitvisibilitychange" -> "Pindah Aplikasi / Layar"
            "blur", "focusout" -> "Gangguan Fokus / Panggilan"
            "pagehide", "freeze" -> "Jeda Latar Belakang"
            else -> "Gangguan Fokus"
        }
        _uiState.update { state ->
            state.copy(
                shieldedCount = if (shouldIncrement) state.shieldedCount + 1 else state.shieldedCount,
                lastShieldedEvent = friendlyLabel,
            )
        }
    }

    fun onPendingExternalUrl(url: String?) {
        _uiState.update { it.copy(pendingExternalUrl = url) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { repository.setKeepScreenOn(enabled) }
    }

    fun setBackgroundKeepAlive(enabled: Boolean) {
        viewModelScope.launch { repository.setBackgroundKeepAlive(enabled) }
    }

    fun setSessionGuardEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSessionGuardEnabled(enabled) }
    }

    fun setForceCopyEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setForceCopyEnabled(enabled) }
    }

    fun setAccidentalExitLock(enabled: Boolean) {
        viewModelScope.launch { repository.setAccidentalExitLock(enabled) }
    }

    fun setDesktopMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDesktopMode(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setAiConfig(baseUrl: String, model: String) {
        viewModelScope.launch { repository.setAiConfig(baseUrl, model) }
    }

    fun updateAiDraftPrompt(text: String) {
        _uiState.update { it.copy(aiDraftPrompt = text) }
    }

    fun sendAiMessage(rawPrompt: String, includePageContext: Boolean = true) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _uiState.value.isAiLoading) return

        val snapshot = _uiState.value
        val userMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            role = AiChatMessage.Role.USER,
            content = prompt,
        )

        _uiState.update {
            it.copy(
                aiMessages = it.aiMessages + userMessage,
                aiDraftPrompt = "",
                isAiLoading = true,
            )
        }

        viewModelScope.launch {
            val result = aiClient.sendChat(
                baseUrl = snapshot.preferences.aiBaseUrl,
                model = snapshot.preferences.aiModel,
                history = snapshot.aiMessages,
                userMessage = prompt,
                pageContextTitle = if (includePageContext && snapshot.screenMode == BrowserScreenMode.ActiveWeb) {
                    snapshot.pageTitle
                } else {
                    ""
                },
            )

            val replyMessage = when (result) {
                is AiResult.Success -> AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = AiChatMessage.Role.ASSISTANT,
                    content = result.data,
                    modelUsed = snapshot.preferences.aiModel,
                    isError = false,
                )

                is AiResult.Error -> AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = AiChatMessage.Role.ASSISTANT,
                    content = result.message,
                    modelUsed = snapshot.preferences.aiModel,
                    isError = true,
                )
            }

            _uiState.update {
                it.copy(
                    aiMessages = it.aiMessages + replyMessage,
                    isAiLoading = false,
                )
            }
        }
    }

    fun fetchAiModels() {
        if (_uiState.value.isFetchingModels) return
        val baseUrl = _uiState.value.preferences.aiBaseUrl
        _uiState.update {
            it.copy(isFetchingModels = true, aiModelStatusMessage = null)
        }
        viewModelScope.launch {
            when (val res = aiClient.fetchModels(baseUrl)) {
                is AiResult.Success -> {
                    val models = res.data.ifEmpty { listOf("gpt-6.1-sol", "gpt-6-astra") }
                    _uiState.update {
                        it.copy(
                            availableAiModels = models,
                            isFetchingModels = false,
                            aiModelStatusMessage = "Berhasil memuat ${models.size} model dari server.",
                        )
                    }
                }

                is AiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isFetchingModels = false,
                            aiModelStatusMessage = res.message,
                        )
                    }
                }
            }
        }
    }

    fun clearAiChat() {
        _uiState.update { it.copy(aiMessages = emptyList()) }
    }

    fun toggleBookmarkForCurrentPage() {
        val currentUrl = _uiState.value.currentUrl
        if (currentUrl.isBlank()) return
        val isSaved = _uiState.value.preferences.bookmarks.any {
            it.url.equals(currentUrl, ignoreCase = true)
        }
        viewModelScope.launch {
            if (isSaved) {
                repository.removeBookmark(currentUrl)
            } else {
                val title = _uiState.value.pageTitle.ifBlank {
                    SessionGuardScript.extractHostDisplay(currentUrl)
                }
                repository.addOrUpdateBookmark(title = title, url = currentUrl)
            }
        }
    }

    fun addBookmark(title: String, rawUrl: String) {
        val normalized = SessionGuardScript.normalizeTargetUrl(rawUrl)
        if (normalized.isBlank()) return
        viewModelScope.launch {
            repository.addOrUpdateBookmark(
                title = title.ifBlank { SessionGuardScript.extractHostDisplay(normalized) },
                url = normalized,
            )
        }
    }

    fun removeBookmark(item: BookmarkItem) {
        viewModelScope.launch { repository.removeBookmark(item.url) }
    }

    fun removeRecentSession(item: RecentSession) {
        viewModelScope.launch { repository.removeRecentSession(item.url) }
    }

    fun clearRecentSessions() {
        viewModelScope.launch { repository.clearRecentSessions() }
    }

    private fun computeElapsedSeconds(): Long {
        if (sessionStartElapsedMs <= 0L) return 0L
        return ((SystemClock.elapsedRealtime() - sessionStartElapsedMs) / 1000L).coerceAtLeast(0L)
    }

    private fun startSessionTimerIfNeeded() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(sessionSeconds = computeElapsedSeconds()) }
                delay(1000L)
            }
        }
    }

    private fun stopSessionTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
