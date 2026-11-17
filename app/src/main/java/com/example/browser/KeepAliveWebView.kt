package com.example.browser

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView

/**
 * Custom hardware-accelerated WebView that keeps Chromium's rendering, focus state,
 * JS timers, and network connections active even when the user turns off the screen
 * or temporarily switches to another app (e.g., replying to an urgent chat).
 * Web page timers continue running honestly in real time without pausing or resetting.
 */
class KeepAliveWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : WebView(context, attrs, defStyleAttr) {

    @Volatile
    var keepAliveInBackground: Boolean = true

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    internal fun resolveEffectiveWindowVisibility(requestedVisibility: Int): Int {
        return if (keepAliveInBackground && requestedVisibility != View.VISIBLE) {
            View.VISIBLE
        } else {
            requestedVisibility
        }
    }

    internal fun resolveEffectiveWindowFocus(hasWindowFocus: Boolean): Boolean {
        return if (keepAliveInBackground) true else hasWindowFocus
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(resolveEffectiveWindowVisibility(visibility))
    }

    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        super.dispatchWindowVisibilityChanged(resolveEffectiveWindowVisibility(visibility))
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(resolveEffectiveWindowFocus(hasWindowFocus))
    }
}
