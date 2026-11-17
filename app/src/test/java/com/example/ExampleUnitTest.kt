package com.example

import com.example.browser.SessionGuardScript
import com.example.ui.components.formatSessionDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun formatSessionDuration_formatsMinutesAndHoursCorrectly() {
        assertEquals("00:05", formatSessionDuration(5L))
        assertEquals("02:15", formatSessionDuration(135L))
        assertEquals("01:01:05", formatSessionDuration(3665L))
    }

    @Test
    fun buildGuardScript_coversFocusGuardAndForceCopyWithoutTouchingTimers() {
        val enabledJs = SessionGuardScript.buildGuardScript(
            isActive = true,
            forceCopyEnabled = true,
        )
        val disabledJs = SessionGuardScript.buildGuardScript(
            isActive = false,
            forceCopyEnabled = false,
        )

        assertTrue(enabledJs.contains("window.__bebasGuardActive = true;"))
        assertTrue(enabledJs.contains("window.__stayFlowForceCopy = true;"))
        assertTrue(disabledJs.contains("window.__bebasGuardActive = false;"))
        assertTrue(disabledJs.contains("window.__stayFlowForceCopy = false;"))

        assertTrue(enabledJs.contains("EventTarget.prototype.addEventListener"))
        assertTrue(enabledJs.contains("blockPropertyHandler(window, 'onblur', 'blur')"))
        assertTrue(enabledJs.contains("blockPropertyHandler(document, 'onvisibilitychange', 'visibilitychange')"))
        assertTrue(enabledJs.contains("user-select: text !important"))
        assertTrue(enabledJs.contains("selectstart"))
        assertTrue(enabledJs.contains(SessionGuardScript.BRIDGE_NAME))

        // Ensure real-time timers are never overridden or paused
        assertFalse(enabledJs.contains("setInterval"))
        assertFalse(enabledJs.contains("setTimeout"))
    }
}
