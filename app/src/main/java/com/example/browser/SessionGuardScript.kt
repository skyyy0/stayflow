package com.example.browser

import android.net.Uri
import android.webkit.JavascriptInterface

object SessionGuardScript {

    const val BRIDGE_NAME = "BebasGuardBridge"

    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    /**
     * Injected at document-start (and reinforced at page lifecycle hooks) across main frame and iframes.
     * 1) Stabilizes Page Visibility & Focus APIs without modifying real-time JS timers.
     * 2) Optionally unlocks text selection, long-press, and copy/contextmenu on restrictive pages.
     */
    fun buildGuardScript(
        isActive: Boolean,
        forceCopyEnabled: Boolean = true,
    ): String {
        val activeLiteral = if (isActive) "true" else "false"
        val forceCopyLiteral = if (forceCopyEnabled) "true" else "false"
        return """
            (function() {
                window.__bebasGuardActive = $activeLiteral;
                window.__stayFlowForceCopy = $forceCopyLiteral;

                var applyForceCopyCss = function(enabled) {
                    try {
                        var styleId = 'stayflow-force-copy-style';
                        var existing = document.getElementById(styleId);
                        if (enabled) {
                            if (!existing) {
                                var style = document.createElement('style');
                                style.id = styleId;
                                style.textContent = '*, *::before, *::after { -webkit-user-select: text !important; user-select: text !important; -webkit-touch-callout: default !important; }';
                                (document.head || document.documentElement).appendChild(style);
                            }
                        } else if (existing && existing.parentNode) {
                            existing.parentNode.removeChild(existing);
                        }
                    } catch (e) {}
                };
                window.__stayFlowApplyForceCopyCss = applyForceCopyCss;

                if (document.documentElement) {
                    applyForceCopyCss(window.__stayFlowForceCopy);
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        applyForceCopyCss(window.__stayFlowForceCopy);
                    }, true);
                }

                if (window.__bebasGuardInstalled) {
                    return;
                }
                window.__bebasGuardInstalled = true;

                var notifyBridge = function(eventType) {
                    try {
                        if (window.$BRIDGE_NAME && typeof window.$BRIDGE_NAME.onInterruptionShielded === 'function') {
                            window.$BRIDGE_NAME.onInterruptionShielded(String(eventType || 'focus'));
                        }
                    } catch (e) {}
                };

                var originalHiddenDesc = Object.getOwnPropertyDescriptor(Document.prototype, 'hidden')
                    || Object.getOwnPropertyDescriptor(document, 'hidden');
                var originalVisDesc = Object.getOwnPropertyDescriptor(Document.prototype, 'visibilityState')
                    || Object.getOwnPropertyDescriptor(document, 'visibilityState');

                try {
                    Object.defineProperty(Document.prototype, 'hidden', {
                        configurable: true,
                        get: function() {
                            if (window.__bebasGuardActive) return false;
                            return originalHiddenDesc && originalHiddenDesc.get ? originalHiddenDesc.get.call(this) : false;
                        }
                    });
                    Object.defineProperty(Document.prototype, 'visibilityState', {
                        configurable: true,
                        get: function() {
                            if (window.__bebasGuardActive) return 'visible';
                            return originalVisDesc && originalVisDesc.get ? originalVisDesc.get.call(this) : 'visible';
                        }
                    });
                    Object.defineProperty(Document.prototype, 'webkitHidden', {
                        configurable: true,
                        get: function() {
                            return window.__bebasGuardActive ? false : document.hidden;
                        }
                    });
                    Object.defineProperty(Document.prototype, 'webkitVisibilityState', {
                        configurable: true,
                        get: function() {
                            return window.__bebasGuardActive ? 'visible' : document.visibilityState;
                        }
                    });
                } catch (e) {}

                var originalHasFocus = Document.prototype.hasFocus;
                Document.prototype.hasFocus = function() {
                    if (window.__bebasGuardActive) return true;
                    return originalHasFocus ? originalHasFocus.apply(this, arguments) : true;
                };

                var guardedEvents = {
                    'visibilitychange': true,
                    'webkitvisibilitychange': true,
                    'blur': true,
                    'focusout': true,
                    'pagehide': true,
                    'freeze': true
                };

                var copyEvents = {
                    'copy': true,
                    'cut': true,
                    'selectstart': true,
                    'contextmenu': true,
                    'dragstart': true
                };

                var isWindowOrDocTarget = function(target) {
                    return target === window || target === document || target === document.documentElement || target === document.body;
                };

                var captureGuard = function(event) {
                    if (!window.__bebasGuardActive) return;
                    var type = event && event.type ? event.type : '';
                    if (!guardedEvents[type]) return;
                    if (type === 'blur' || type === 'focusout') {
                        if (!isWindowOrDocTarget(event.target)) {
                            return;
                        }
                    }
                    event.stopImmediatePropagation();
                    event.stopPropagation();
                    notifyBridge(type);
                };

                var captureCopyUnlock = function(event) {
                    if (!window.__stayFlowForceCopy) return;
                    var type = event && event.type ? event.type : '';
                    if (!copyEvents[type]) return;
                    // Stop page-level anti-copy listeners without calling preventDefault so native selection/copy works
                    event.stopPropagation();
                };

                var originalAddEventListener = EventTarget.prototype.addEventListener;
                EventTarget.prototype.addEventListener = function(type, listener, options) {
                    if (guardedEvents[type] && typeof listener === 'function' && (this === window || this === document)) {
                        var wrappedFocus = function(evt) {
                            if (window.__bebasGuardActive) {
                                if ((type === 'blur' || type === 'focusout') && evt && !isWindowOrDocTarget(evt.target)) {
                                    return listener.call(this, evt);
                                }
                                notifyBridge(type);
                                return;
                            }
                            return listener.call(this, evt);
                        };
                        return originalAddEventListener.call(this, type, wrappedFocus, options);
                    }
                    if (copyEvents[type] && typeof listener === 'function') {
                        var wrappedCopy = function(evt) {
                            if (window.__stayFlowForceCopy) {
                                return;
                            }
                            return listener.call(this, evt);
                        };
                        return originalAddEventListener.call(this, type, wrappedCopy, options);
                    }
                    return originalAddEventListener.call(this, type, listener, options);
                };

                Object.keys(guardedEvents).forEach(function(evtName) {
                    originalAddEventListener.call(window, evtName, captureGuard, true);
                    originalAddEventListener.call(document, evtName, captureGuard, true);
                });

                Object.keys(copyEvents).forEach(function(evtName) {
                    originalAddEventListener.call(window, evtName, captureCopyUnlock, true);
                    originalAddEventListener.call(document, evtName, captureCopyUnlock, true);
                });

                var blockPropertyHandler = function(target, propName, evtType) {
                    try {
                        var backing = null;
                        Object.defineProperty(target, propName, {
                            configurable: true,
                            get: function() { return window.__bebasGuardActive ? null : backing; },
                            set: function(fn) {
                                backing = typeof fn === 'function' ? fn : null;
                                if (backing) {
                                    target.addEventListener(evtType, backing);
                                }
                            }
                        });
                    } catch (e) {}
                };

                blockPropertyHandler(window, 'onblur', 'blur');
                blockPropertyHandler(window, 'onfocusout', 'focusout');
                blockPropertyHandler(window, 'onpagehide', 'pagehide');
                blockPropertyHandler(document, 'onvisibilitychange', 'visibilitychange');
                blockPropertyHandler(document, 'onwebkitvisibilitychange', 'webkitvisibilitychange');
            })();
        """.trimIndent()
    }

    fun buildToggleJs(isActive: Boolean, forceCopyEnabled: Boolean): String {
        val activeLiteral = if (isActive) "true" else "false"
        val copyLiteral = if (forceCopyEnabled) "true" else "false"
        return """
            window.__bebasGuardActive = $activeLiteral;
            window.__stayFlowForceCopy = $copyLiteral;
            if (typeof window.__stayFlowApplyForceCopyCss === 'function') {
                window.__stayFlowApplyForceCopyCss($copyLiteral);
            }
        """.trimIndent()
    }

    fun normalizeTargetUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        val hasSpaces = trimmed.contains(' ')
        val looksLikeHost = !hasSpaces && (
            trimmed.contains('.') ||
                trimmed.startsWith("localhost", ignoreCase = true)
            )
        return if (looksLikeHost) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${Uri.encode(trimmed)}"
        }
    }

    fun extractHostDisplay(url: String): String {
        if (url.isBlank()) return ""
        return try {
            val uri = Uri.parse(url)
            uri.host?.removePrefix("www.") ?: url
        } catch (_: Exception) {
            url
        }
    }
}

class BebasGuardJsBridge(
    private val onShielded: (String) -> Unit,
) {
    @JavascriptInterface
    fun onInterruptionShielded(eventType: String) {
        onShielded(eventType)
    }
}
