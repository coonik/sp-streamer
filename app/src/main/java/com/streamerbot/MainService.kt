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

                val popup = findPopupParent(root, "Vòng quay")
                if (popup != null) {
                    spamClickCenter(popup)
                    continue
                }

                val goButton = getGoButton(root)
                if (goButton != null) {
                    Thread.sleep(5000)

                    val fl = findPartialText(root, "Theo dõi")
                    if (fl != null) {
                        fl.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    goButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    continue
                }

                val xuStreamer = findText(root, "Xu Streamer")
                if (xuStreamer != null) {
                    val nhan = findPartialText(root, "Lưu")
                    if (nhan != null) {
                        nhan.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Thread.sleep(2000)
                        performScrollOrSwipe()
                    }

                    val countdownText = findCountdownNear(xuStreamer)
                    if (countdownText != null) {
                        val minutes = extractMinutes(countdownText)
                        if (minutes > 5) {
                            performScrollOrSwipe()
                        }
                    }
                } else {
                    performScrollOrSwipe()
                }
                Thread.sleep(4000)
            }
        }.start()
    }

    private fun getGoButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val countdownNodes = mutableListOf<AccessibilityNodeInfo>()
        collectCountdownNodes(root, countdownNodes)

        if (countdownNodes.isEmpty()) return null

        val lastCountdown = countdownNodes[countdownNodes.size - 1]

        if (countdownNodes.size == 3 || (countdownNodes.size == 2 && findPartialText(root, "Xu Streamer") == null)) {
            return lastCountdown.parent
        }
        return null
    }

    private fun collectCountdownNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return

        val text = node.text?.toString()
        if (text != null && text.matches(Regex("\\d{1,2}:\\d{2}"))) {
            list.add(node)
        }

        for (i in 0 until node.childCount) {
            collectCountdownNodes(node.getChild(i), list)
        }
    }

    private fun findPopupParent(node: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString()?.contains(keyword) == true) {
            return node.parent ?: node
        }
        for (i in 0 until node.childCount) {
            val result = findPopupParent(node.getChild(i), keyword)
            if (result != null) return result
        }
        return null
    }

    private fun spamClickCenter(node: AccessibilityNodeInfo) {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)

        // Nếu bounds là rỗng, bỏ qua không click
        if (bounds.isEmpty) {
            return
        }

        // Vị trí giữa của bounds
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        // Tạo path hình tròn có đường kính 10px
        val radius = 5f // bán kính 5px, tức là đường kính 10px

        val path = Path().apply {
            moveTo(centerX, centerY)
            // Vẽ một vòng tròn nhỏ
            addCircle(centerX, centerY, radius, Path.Direction.CW)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100)) // click trong 100ms
            .build()

        // Gửi gesture click
        dispatchGesture(gesture, null, null)
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
        val scrolled = performGlobalAction(4096)
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
        val startY = (screenHeight * 0.7).toFloat()
        val endY = (screenHeight * 0.4).toFloat()

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()

        dispatchGesture(gesture, null, null)
    }
}
