package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

/**
 * Custom hardware-accelerated WebView that:
 * 1) Preserves standard WebView style & touch focusability (android.R.attr.webViewStyle)
 *    so tapping <input>/<textarea> reliably opens the Android soft keyboard (IME).
 * 2) Keeps Chromium's rendering, focus state, JS timers, and network connections active
 *    even when the user turns off the screen or switches apps.
 */
class KeepAliveWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle,
) : WebView(context, attrs, defStyleAttr) {

    @Volatile
    var keepAliveInBackground: Boolean = true

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN && !hasFocus()) {
            requestFocus()
        }
        return super.onTouchEvent(event)
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
