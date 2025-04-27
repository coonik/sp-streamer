package com.streamerbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MainService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        startChecking()
    }

    private fun startChecking() {
        Thread {
            while (true) {
                val root = rootInActiveWindow ?: continue

                val xuStreamer = findText(root, "Xu Streamer")
                if (xuStreamer != null) {
                    val nhan = findPartialText(root, "Lưu")
                    if (nhan != null) {
                        nhan.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                } else {
                    swipeUp()
                }

                Thread.sleep(2000)
            }
        }.start()
    }

    private fun findText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString()?.contains(text) == true) return node
        for (i in 0 until node.childCount) {
            val result = findText(node.getChild(i), text)
            if (result != null) return result
        }
        return null
    }

    private fun findPartialText(node: AccessibilityNodeInfo?, partial: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString()?.startsWith(partial) == true) return node
        for (i in 0 until node.childCount) {
            val result = findPartialText(node.getChild(i), partial)
            if (result != null) return result
        }
        return null
    }

    private fun swipeUp() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val centerX = (screenWidth / 2).toFloat()
        val startY = (screenHeight * 0.6f)
        val endY = (screenHeight * 0.3f)

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500)) // vuốt trong 500ms
            .build()

        dispatchGesture(gesture, null, null)
    }
}
