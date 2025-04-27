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
                    val countdownText = findCountdownNear(xuStreamer)
                    if (countdownText != null) {
                        val minutes = extractMinutes(countdownText)
                        if (minutes > 5) {
                            performScrollOrSwipe()
                        }
                    }
                    val nhan = findPartialText(root, "Lưu")
                    if (nhan != null) {
                        nhan.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                } else {
                    performScrollOrSwipe()
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

    private fun performScrollOrSwipe() {
        val scrolled = performGlobalAction(AccessibilityService.GLOBAL_ACTION_SCROLL_FORWARD)
        if (!scrolled) {
            swipeManually()
        }
    }

    private fun findCountdownNear(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i)
            val text = child?.text?.toString()
            if (text != null && text.matches(Regex("\\d{1,2}:\\d{2}"))) {
                return text
            }
        }
        return null
    }

    private fun extractMinutes(countdown: String): Int {
        val parts = countdown.split(":")
        return parts.getOrNull(0)?.toIntOrNull() ?: 0
    }

    private fun swipeManually() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val startX = (screenWidth / 2).toFloat()
        val startY = (screenHeight * 0.7).toFloat()  // Bắt đầu từ 70% chiều cao
        val endY = (screenHeight * 0.4).toFloat()    // Vuốt lên tới 40% chiều cao

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500)) // 500ms vuốt cho nhẹ
            .build()

        dispatchGesture(gesture, null, null)
    }

}
