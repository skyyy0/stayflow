package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.browser.BebasGuardJsBridge
import com.example.browser.KeepAliveWebView
import com.example.browser.SessionGuardScript
import com.example.ui.components.AiChatContent
import com.example.ui.components.formatSessionDuration
import com.example.ui.components.liquidPressScale
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.BebasUiState
import kotlinx.coroutines.delay
import org.json.JSONTokener

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    uiState: BebasUiState,
    reloadTrigger: Int,
    clearCacheTrigger: Int,
    onOpenUrl: (String) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onProgressChanged: (progress: Int, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onPageTitleReceived: (String) -> Unit,
    onLoadError: (String?) -> Unit,
    onClearLoadError: () -> Unit,
    onInterruptionShielded: (String) -> Unit,
    onPendingExternalUrl: (String?) -> Unit,
    onToggleBookmark: () -> Unit,
    onAiDraftChange: (String) -> Unit,
    onSendAiMessage: (String) -> Unit,
    onClearAiChat: () -> Unit,
    onSelectAiModel: (String) -> Unit,
    onOpenShieldSheet: () -> Unit,
    onExitToHome: () -> Unit,
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var showEditUrlDialog by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var showBackHint by remember { mutableStateOf(false) }
    var lastBackTapMs by remember { mutableLongStateOf(0L) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var docStartScriptHandler by remember { mutableStateOf<ScriptHandler?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingFileCallback?.onReceiveValue(uris.toTypedArray())
        } else {
            pendingFileCallback?.onReceiveValue(null)
        }
        pendingFileCallback = null
    }

    val defaultUserAgent = remember { WebSettings.getDefaultUserAgent(context) }

    // Retain a single KeepAliveWebView instance across recompositions, screen-off, and app switches
    val webView = remember {
        KeepAliveWebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            keepAliveInBackground = uiState.preferences.backgroundKeepAlive
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowFileAccess = true
                userAgentString = if (uiState.preferences.desktopMode) {
                    SessionGuardScript.DESKTOP_USER_AGENT
                } else {
                    defaultUserAgent
                }
            }
            val currentWebView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(currentWebView, true)
            }
            addJavascriptInterface(
                BebasGuardJsBridge { eventType ->
                    mainHandler.post { onInterruptionShielded(eventType) }
                },
                SessionGuardScript.BRIDGE_NAME,
            )
        }
    }

    // Keep WebView background keep-alive synced with user preference
    LaunchedEffect(uiState.preferences.backgroundKeepAlive) {
        webView.keepAliveInBackground = uiState.preferences.backgroundKeepAlive
    }

    // Register DOCUMENT_START_SCRIPT across main frame & iframes BEFORE any page JS executes
    val guardActiveRef = remember { mutableStateOf(uiState.preferences.sessionGuardEnabled) }
    val forceCopyRef = remember { mutableStateOf(uiState.preferences.forceCopyEnabled) }
    LaunchedEffect(
        webView,
        uiState.preferences.sessionGuardEnabled,
        uiState.preferences.forceCopyEnabled,
    ) {
        guardActiveRef.value = uiState.preferences.sessionGuardEnabled
        forceCopyRef.value = uiState.preferences.forceCopyEnabled
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                docStartScriptHandler?.remove()
                docStartScriptHandler = WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    SessionGuardScript.buildGuardScript(
                        isActive = uiState.preferences.sessionGuardEnabled,
                        forceCopyEnabled = uiState.preferences.forceCopyEnabled,
                    ),
                    setOf("*"),
                )
            } catch (_: Exception) {
            }
        }
        webView.evaluateJavascript(
            SessionGuardScript.buildToggleJs(
                isActive = uiState.preferences.sessionGuardEnabled,
                forceCopyEnabled = uiState.preferences.forceCopyEnabled,
            ),
            null,
        )
    }

    // Update User-Agent when desktopMode toggles
    val initialDesktopState = remember { mutableStateOf(uiState.preferences.desktopMode) }
    LaunchedEffect(uiState.preferences.desktopMode) {
        if (initialDesktopState.value != uiState.preferences.desktopMode) {
            initialDesktopState.value = uiState.preferences.desktopMode
            webView.settings.userAgentString = if (uiState.preferences.desktopMode) {
                SessionGuardScript.DESKTOP_USER_AGENT
            } else {
                defaultUserAgent
            }
            webView.reload()
        }
    }

    // Load initial or newly requested URL only when currentUrl changes programmatically
    var lastRequestedUrl by remember { mutableStateOf("") }
    LaunchedEffect(uiState.currentUrl) {
        if (uiState.currentUrl.isNotBlank() &&
            uiState.currentUrl != lastRequestedUrl &&
            uiState.currentUrl != webView.url
        ) {
            lastRequestedUrl = uiState.currentUrl
            webView.loadUrl(uiState.currentUrl)
        }
    }

    // Handle reload trigger from ShieldControlSheet
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            onClearLoadError()
            webView.reload()
        }
    }

    // Handle cache clear trigger from ShieldControlSheet
    LaunchedEffect(clearCacheTrigger) {
        if (clearCacheTrigger > 0) {
            webView.clearCache(true)
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            webView.reload()
        }
    }

    // Configure WebView clients once
    DisposableEffect(webView) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val targetUrl = request?.url?.toString() ?: return false
                if (targetUrl.startsWith("http://", ignoreCase = true) ||
                    targetUrl.startsWith("https://", ignoreCase = true)
                ) {
                    return false
                }
                onPendingExternalUrl(targetUrl)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!url.isNullOrBlank()) {
                    lastRequestedUrl = url
                    onPageStarted(url)
                }
                view?.evaluateJavascript(
                    SessionGuardScript.buildGuardScript(
                        isActive = guardActiveRef.value,
                        forceCopyEnabled = forceCopyRef.value,
                    ),
                    null,
                )
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                view?.evaluateJavascript(
                    SessionGuardScript.buildGuardScript(
                        isActive = guardActiveRef.value,
                        forceCopyEnabled = forceCopyRef.value,
                    ),
                    null,
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(
                    SessionGuardScript.buildGuardScript(
                        isActive = guardActiveRef.value,
                        forceCopyEnabled = forceCopyRef.value,
                    ),
                    null,
                )
                if (!url.isNullOrBlank()) {
                    lastRequestedUrl = url
                    onPageFinished(
                        url,
                        view?.title ?: "",
                        view?.canGoBack() ?: false,
                        view?.canGoForward() ?: false,
                    )
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    onLoadError(error?.description?.toString())
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(
                    newProgress,
                    view?.canGoBack() ?: false,
                    view?.canGoForward() ?: false,
                )
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank()) {
                    onPageTitleReceived(title)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = filePathCallback
                val mimeTypes = fileChooserParams?.acceptTypes
                    ?.filter { it.isNotBlank() }
                    ?.takeIf { it.isNotEmpty() }
                    ?.toTypedArray()
                    ?: arrayOf("*/*")
                return try {
                    filePickerLauncher.launch(mimeTypes)
                    true
                } catch (_: Exception) {
                    pendingFileCallback?.onReceiveValue(null)
                    pendingFileCallback = null
                    false
                }
            }
        }

        onDispose {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            try {
                docStartScriptHandler?.remove()
            } catch (_: Exception) {
            }
            docStartScriptHandler = null
            webView.keepAliveInBackground = false
            webView.stopLoading()
            webView.destroy()
        }
    }

    // Auto-hide accidental back confirmation hint after 2.2 seconds
    LaunchedEffect(showBackHint) {
        if (showBackHint) {
            delay(2200L)
            showBackHint = false
        }
    }

    // Protective BackHandler (only prevents closing tab to Home; Home/Recents app switching is always free)
    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack()
        } else if (uiState.preferences.accidentalExitLock) {
            val now = System.currentTimeMillis()
            if (now - lastBackTapMs < 2200L) {
                onExitToHome()
            } else {
                lastBackTapMs = now
                showBackHint = true
            }
        } else {
            onExitToHome()
        }
    }

    val isBookmarked = uiState.preferences.bookmarks.any {
        it.url.equals(uiState.currentUrl, ignoreCase = true)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (uiState.loadingProgress / 100f).coerceIn(0.05f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "webview_progress",
    )

    val glassSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.90f else 0.94f)
    val glassBorderBrush = remember(isDark) {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) 0.16f else 0.65f),
                Color.White.copy(alpha = if (isDark) 0.06f else 0.30f),
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Liquid Glass Top Compact Bar
        Surface(
            color = glassSurfaceColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShieldStatusPill(
                        isGuardEnabled = uiState.preferences.sessionGuardEnabled,
                        shieldedCount = uiState.shieldedCount,
                        onClick = onOpenShieldSheet,
                    )

                    // Address / Domain Glass Pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, glassBorderBrush),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .liquidPressScale(pressedScale = 0.98f)
                            .clickable { showEditUrlDialog = true }
                            .testTag("browser_address_bar"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (uiState.isSslSecure) {
                                    Icons.Rounded.Lock
                                } else {
                                    Icons.Rounded.LockOpen
                                },
                                contentDescription = if (uiState.isSslSecure) "HTTPS Aman" else "HTTP",
                                tint = if (uiState.isSslSecure) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = SessionGuardScript.extractHostDisplay(uiState.currentUrl)
                                    .ifBlank { uiState.currentUrl },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (uiState.isLoading) {
                                webView.stopLoading()
                            } else {
                                onClearLoadError()
                                webView.reload()
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .liquidPressScale()
                            .testTag("browser_reload_button"),
                    ) {
                        Icon(
                            imageVector = if (uiState.isLoading) {
                                Icons.Rounded.Close
                            } else {
                                Icons.Rounded.Refresh
                            },
                            contentDescription = if (uiState.isLoading) "Hentikan" else "Muat ulang",
                        )
                    }
                }

                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

        // Main WebView Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )

            // Error Overlay
            if (uiState.loadErrorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(52.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Halaman Tidak Dapat Dimuat",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.loadErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onExitToHome) {
                                Text("Beranda")
                            }
                            Button(
                                onClick = {
                                    onClearLoadError()
                                    webView.reload()
                                },
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
            }

            // Accidental Back Press Guard Toast
            androidx.compose.animation.AnimatedVisibility(
                visible = showBackHint,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    ),
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Tekan kembali sekali lagi untuk menutup tab ke Beranda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Liquid Glass Bottom Control Bar
        Surface(
            color = glassSurfaceColor,
            tonalElevation = 4.dp,
            border = BorderStroke(0.5.dp, glassBorderBrush),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { if (webView.canGoBack()) webView.goBack() },
                    enabled = uiState.canGoBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .liquidPressScale(enabled = uiState.canGoBack)
                        .testTag("browser_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Halaman sebelumnya",
                    )
                }

                IconButton(
                    onClick = { if (webView.canGoForward()) webView.goForward() },
                    enabled = uiState.canGoForward,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .liquidPressScale(enabled = uiState.canGoForward)
                        .testTag("browser_forward_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Halaman berikutnya",
                    )
                }

                // Dynamic Island-inspired Live Real-Time Session Timer + Shield + Keep-Alive Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .liquidPressScale()
                        .clickable(onClick = onOpenShieldSheet)
                        .animateContentSize(spring(stiffness = Spring.StiffnessMedium))
                        .testTag("browser_timer_shield_pill"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "Buka Pengaturan StayFlow",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = formatSessionDuration(uiState.sessionSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (uiState.preferences.backgroundKeepAlive) {
                            Icon(
                                imageVector = Icons.Rounded.AllInclusive,
                                contentDescription = "Sesi latar aktif",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        webView.evaluateJavascript(
                            "(function(){try{return window.getSelection?window.getSelection().toString():'';}catch(e){return '';}})()",
                        ) { raw ->
                            val selected = try {
                                (JSONTokener(raw).nextValue() as? String)?.trim().orEmpty()
                            } catch (_: Exception) {
                                ""
                            }
                            if (selected.isNotBlank()) {
                                onAiDraftChange(selected)
                            }
                            showAiSheet = true
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .liquidPressScale()
                        .testTag("browser_ai_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Tanya AI tentang soal di halaman ini",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .liquidPressScale()
                        .testTag("browser_bookmark_button"),
                ) {
                    Icon(
                        imageVector = if (isBookmarked) {
                            Icons.Rounded.Bookmark
                        } else {
                            Icons.Rounded.BookmarkBorder
                        },
                        contentDescription = if (isBookmarked) {
                            "Hapus dari tautan tersimpan"
                        } else {
                            "Simpan tautan"
                        },
                        tint = if (isBookmarked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                IconButton(
                    onClick = onExitToHome,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .liquidPressScale()
                        .testTag("browser_home_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = "Kembali ke Beranda StayFlow",
                    )
                }
            }
        }
    }

    if (showAiSheet) {
        val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false },
            sheetState = aiSheetState,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.76f)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tanya AI StayFlow",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Halaman web tetap aktif di belakang",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { showAiSheet = false }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup panel AI",
                        )
                    }
                }

                AiChatContent(
                    uiState = uiState,
                    onDraftChange = onAiDraftChange,
                    onSendMessage = onSendAiMessage,
                    onClearChat = onClearAiChat,
                    onSelectModel = onSelectAiModel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showEditUrlDialog) {
        EditUrlDialog(
            initialUrl = uiState.currentUrl,
            onDismiss = { showEditUrlDialog = false },
            onNavigate = { raw ->
                showEditUrlDialog = false
                val target = SessionGuardScript.normalizeTargetUrl(raw)
                if (target.isNotBlank()) {
                    lastRequestedUrl = target
                    onOpenUrl(target)
                    webView.loadUrl(target)
                }
            },
        )
    }

    val pendingExternal = uiState.pendingExternalUrl
    if (pendingExternal != null) {
        AlertDialog(
            onDismissRequest = { onPendingExternalUrl(null) },
            title = { Text("Tautan Aplikasi Eksternal") },
            text = {
                Text(
                    text = "Halaman mencoba membuka aplikasi lain ($pendingExternal). Sesi Anda di StayFlow tetap tersimpan jika ingin membukanya.",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPendingExternalUrl(null)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pendingExternal))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                ) {
                    Text("Buka")
                }
            },
            dismissButton = {
                TextButton(onClick = { onPendingExternalUrl(null) }) {
                    Text("Tetap di Sini")
                }
            },
        )
    }
}

@Composable
private fun ShieldStatusPill(
    isGuardEnabled: Boolean,
    shieldedCount: Int,
    onClick: () -> Unit,
) {
    val pulseTransition = rememberInfiniteTransition(label = "shield_pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )

    Surface(
        shape = CircleShape,
        color = if (isGuardEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isGuardEnabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            },
        ),
        modifier = Modifier
            .height(44.dp)
            .clip(CircleShape)
            .liquidPressScale()
            .clickable(onClick = onClick)
            .animateContentSize(spring(stiffness = Spring.StiffnessMedium))
            .testTag("browser_shield_pill"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(if (isGuardEnabled) dotAlpha else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isGuardEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
            )
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = "Status Perisai",
                tint = if (isGuardEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(16.dp),
            )
            if (shieldedCount > 0) {
                Text(
                    text = "$shieldedCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun EditUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    var draftUrl by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buka Alamat Web") },
        text = {
            OutlinedTextField(
                value = draftUrl,
                onValueChange = { draftUrl = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onNavigate(draftUrl) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_url_dialog_input"),
            )
        },
        confirmButton = {
            Button(
                onClick = { onNavigate(draftUrl) },
                enabled = draftUrl.isNotBlank(),
            ) {
                Text("Buka")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}
