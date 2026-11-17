package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.browser.SessionGuardScript
import com.example.service.SessionKeepAliveService
import com.example.ui.components.LiquidBottomTabBar
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidNavigationRail
import com.example.ui.components.ShieldControlSheet
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BebasTheme
import com.example.viewmodel.BebasViewModel
import com.example.viewmodel.BrowserScreenMode
import com.example.viewmodel.HomeTab

class MainActivity : ComponentActivity() {

    private val viewModel: BebasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showShieldSheet by remember { mutableStateOf(false) }
            var reloadTrigger by remember { mutableIntStateOf(0) }
            var clearCacheTrigger by remember { mutableIntStateOf(0) }
            var askedNotificationPermission by remember { mutableStateOf(false) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { _ -> }

            // Dynamically keep screen awake when Keep Screen On is enabled during an active web session
            LaunchedEffect(uiState.preferences.keepScreenOn, uiState.screenMode) {
                val shouldKeepAwake = uiState.preferences.keepScreenOn &&
                    uiState.screenMode == BrowserScreenMode.ActiveWeb
                if (shouldKeepAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Keep foreground dataSync + PARTIAL_WAKE_LOCK service alive during active web sessions
            LaunchedEffect(
                uiState.screenMode,
                uiState.preferences.backgroundKeepAlive,
                uiState.currentUrl,
            ) {
                val shouldRunService = uiState.screenMode == BrowserScreenMode.ActiveWeb &&
                    uiState.preferences.backgroundKeepAlive
                if (shouldRunService) {
                    if (!askedNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            askedNotificationPermission = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    val hostLabel = SessionGuardScript.extractHostDisplay(uiState.currentUrl)
                    SessionKeepAliveService.start(this@MainActivity, hostLabel)
                } else {
                    SessionKeepAliveService.stop(this@MainActivity)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    SessionKeepAliveService.stop(this@MainActivity)
                }
            }

            BebasTheme(themeMode = uiState.preferences.themeMode) {
                AnimatedContent(
                    targetState = uiState.screenMode,
                    transitionSpec = {
                        (fadeIn(tween(240)) + scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )) togetherWith (fadeOut(tween(180)) + scaleOut(targetScale = 0.98f))
                    },
                    label = "screen_mode_transition",
                ) { mode ->
                    when (mode) {
                        BrowserScreenMode.Home -> {
                            BackHandler(enabled = uiState.activeTab != HomeTab.EXPLORE) {
                                viewModel.selectHomeTab(HomeTab.EXPLORE)
                            }

                            LiquidGlassBackdrop(modifier = Modifier.fillMaxSize()) {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val isWideLayout = maxWidth >= 600.dp
                                    val navBarsBottom = WindowInsets.navigationBars
                                        .asPaddingValues()
                                        .calculateBottomPadding()
                                    val contentBottomPadding = if (isWideLayout) {
                                        PaddingValues(bottom = navBarsBottom)
                                    } else {
                                        PaddingValues(bottom = navBarsBottom + 88.dp)
                                    }

                                    Row(modifier = Modifier.fillMaxSize()) {
                                        if (isWideLayout) {
                                            LiquidNavigationRail(
                                                activeTab = uiState.activeTab,
                                                onSelectTab = viewModel::selectHomeTab,
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxSize()
                                                .statusBarsPadding(),
                                        ) {
                                            AnimatedContent(
                                                targetState = uiState.activeTab,
                                                transitionSpec = {
                                                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                                                },
                                                label = "home_tab_transition",
                                            ) { tab ->
                                                when (tab) {
                                                    HomeTab.EXPLORE -> ExploreScreen(
                                                        uiState = uiState,
                                                        bottomInsetPadding = contentBottomPadding,
                                                        onUrlInputChange = viewModel::updateUrlInput,
                                                        onOpenUrl = { raw -> viewModel.openUrl(raw) },
                                                        onJumpToTab = viewModel::selectHomeTab,
                                                    )

                                                    HomeTab.SAVED -> SavedScreen(
                                                        uiState = uiState,
                                                        bottomInsetPadding = contentBottomPadding,
                                                        onOpenUrl = { raw -> viewModel.openUrl(raw) },
                                                        onAddBookmark = viewModel::addBookmark,
                                                        onRemoveBookmark = viewModel::removeBookmark,
                                                        onRemoveRecentSession = viewModel::removeRecentSession,
                                                        onClearRecentSessions = viewModel::clearRecentSessions,
                                                    )

                                                    HomeTab.AI -> AiAssistantScreen(
                                                        uiState = uiState,
                                                        bottomInsetPadding = contentBottomPadding,
                                                        onDraftChange = viewModel::updateAiDraftPrompt,
                                                        onSendMessage = { prompt ->
                                                            viewModel.sendAiMessage(
                                                                rawPrompt = prompt,
                                                                includePageContext = false,
                                                            )
                                                        },
                                                        onClearChat = viewModel::clearAiChat,
                                                        onSelectModel = { model ->
                                                            viewModel.setAiConfig(
                                                                baseUrl = uiState.preferences.aiBaseUrl,
                                                                model = model,
                                                            )
                                                        },
                                                        onOpenSettingsTab = viewModel::selectHomeTab,
                                                    )

                                                    HomeTab.SETTINGS -> SettingsScreen(
                                                        uiState = uiState,
                                                        bottomInsetPadding = contentBottomPadding,
                                                        onToggleBackgroundKeepAlive = viewModel::setBackgroundKeepAlive,
                                                        onToggleSessionGuard = viewModel::setSessionGuardEnabled,
                                                        onToggleKeepScreenOn = viewModel::setKeepScreenOn,
                                                        onToggleForceCopy = viewModel::setForceCopyEnabled,
                                                        onToggleAccidentalExitLock = viewModel::setAccidentalExitLock,
                                                        onToggleDesktopMode = viewModel::setDesktopMode,
                                                        onSelectThemeMode = viewModel::setThemeMode,
                                                        onSaveAiConfig = viewModel::setAiConfig,
                                                        onFetchAiModels = viewModel::fetchAiModels,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (!isWideLayout) {
                                        LiquidBottomTabBar(
                                            activeTab = uiState.activeTab,
                                            onSelectTab = viewModel::selectHomeTab,
                                            modifier = Modifier.align(Alignment.BottomCenter),
                                        )
                                    }
                                }
                            }
                        }

                        BrowserScreenMode.ActiveWeb -> {
                            BrowserScreen(
                                uiState = uiState,
                                reloadTrigger = reloadTrigger,
                                clearCacheTrigger = clearCacheTrigger,
                                onOpenUrl = { raw -> viewModel.openUrl(raw) },
                                onPageStarted = viewModel::onPageStarted,
                                onPageFinished = viewModel::onPageFinished,
                                onProgressChanged = viewModel::onProgressChanged,
                                onPageTitleReceived = viewModel::onPageTitleReceived,
                                onLoadError = viewModel::onLoadError,
                                onClearLoadError = viewModel::clearLoadError,
                                onInterruptionShielded = viewModel::onInterruptionShielded,
                                onPendingExternalUrl = viewModel::onPendingExternalUrl,
                                onToggleBookmark = viewModel::toggleBookmarkForCurrentPage,
                                onAiDraftChange = viewModel::updateAiDraftPrompt,
                                onSendAiMessage = { prompt ->
                                    viewModel.sendAiMessage(
                                        rawPrompt = prompt,
                                        includePageContext = true,
                                    )
                                },
                                onClearAiChat = viewModel::clearAiChat,
                                onSelectAiModel = { model ->
                                    viewModel.setAiConfig(
                                        baseUrl = uiState.preferences.aiBaseUrl,
                                        model = model,
                                    )
                                },
                                onOpenShieldSheet = { showShieldSheet = true },
                                onExitToHome = {
                                    SessionKeepAliveService.stop(this@MainActivity)
                                    viewModel.returnHome()
                                },
                            )
                        }
                    }
                }

                if (showShieldSheet) {
                    ShieldControlSheet(
                        uiState = uiState,
                        onDismiss = { showShieldSheet = false },
                        onToggleKeepScreenOn = viewModel::setKeepScreenOn,
                        onToggleBackgroundKeepAlive = viewModel::setBackgroundKeepAlive,
                        onToggleSessionGuard = viewModel::setSessionGuardEnabled,
                        onToggleForceCopy = viewModel::setForceCopyEnabled,
                        onToggleAccidentalExitLock = viewModel::setAccidentalExitLock,
                        onToggleDesktopMode = viewModel::setDesktopMode,
                        onSelectThemeMode = viewModel::setThemeMode,
                        onReloadPage = { reloadTrigger++ },
                        onClearSessionCache = { clearCacheTrigger++ },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Immediately sync real-time elapsed clock upon returning from screen-off or another app
        viewModel.syncSessionClock()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val incomingUrl = intent.dataString
            if (!incomingUrl.isNullOrBlank()) {
                viewModel.openUrl(incomingUrl)
            }
        }
    }
}
