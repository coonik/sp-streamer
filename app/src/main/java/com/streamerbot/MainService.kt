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

                val popup = findPartialText(root, "Vòng quay")
                if (popup != null) {
                    val quayButton = findPartialText(root, "QUAY")
                    if (quayButton != null) {
                        quayButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Thread.sleep(3000)
                        findAndClickCloseButton(root)
                    } else {
                        Thread.sleep(1)
                    }
                    continue
                }

                val goButton = getGoButton(root)
                if (goButton != null) {
                    val fl = findPartialText(root, "Theo dõi")
                    if (fl != null) {
                        fl.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    goButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Thread.sleep(5000)
                    continue
                }

                val xuStreamer = findText(root, "Xu Streamer")
                if (xuStreamer != null) {
                    val nhan = findPartialText(root, "Lưu")
                    if (nhan != null) {
                        nhan.performAction(AccessibilityNodeInfo.ACTION_CLICK)
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
                Thread.sleep(3000)
            }
        }.start()
    }

    // New code to close QUAY modal
    private fun findAndClickCloseButton(root: AccessibilityNodeInfo) {
        // Tìm nút đóng (icon X) trong toàn bộ màn hình
        val closeButton = findCloseButton(root)
        if (closeButton != null) {
            closeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    // Tìm nút "Close" hoặc icon "X"
    private fun findCloseButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        // Duyệt qua tất cả các con của node để tìm icon đóng
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val contentDescription = child?.contentDescription?.toString()

            // Kiểm tra nếu nó là nút đóng (icon "X" hoặc "Close")
            if (contentDescription != null && 
                (contentDescription.contains("Close", ignoreCase = true) || 
                contentDescription.contains("Đóng", ignoreCase = true) || 
                contentDescription.contains("X", ignoreCase = true))) {
                return child
            }
        }
        return null
    }

    // END

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
