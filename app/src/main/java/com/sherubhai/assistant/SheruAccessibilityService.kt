package com.sherubhai.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class SheruAccessibilityService : AccessibilityService() {
    companion object { var active: SheruAccessibilityService? = null }
    override fun onServiceConnected() { super.onServiceConnected(); active = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { if (active === this) active = null; super.onDestroy() }
    fun navigate(action: Int): Boolean = performGlobalAction(action)
    fun scrollDown(): Boolean {
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val path = Path().apply { moveTo(x, dm.heightPixels * 0.78f); lineTo(x, dm.heightPixels * 0.28f) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 420)
        return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }
}
