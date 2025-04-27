package com.streamerbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.res.Resources

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
                root.refresh()

                val popup = findNodeByText(root, "vòng quay")
                if (popup != null) {
                    val bdt = findNodeByText(root, "bắt đầu trong")
                    if (bdt != null) {
                        continue
                    }
                    findButtonNearCenter(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Thread.sleep(10)
                    continue
                }

                findCloseButton(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                val goButton = getGoButton(root)
                if (goButton != null) {
                    findNodeByText(root, "theo dõi")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    goButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Thread.sleep(5000)
                    continue
                }

                val xuStreamer = findNodeByText(root, "xu streamer")
                if (xuStreamer != null) {
                    findNodeByText(root, "lưu")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

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

    // Tìm node clickable ở gần giữa màn hình (~2cm bán kính)
    private fun findButtonNearCenter(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val allNodes = ArrayList<AccessibilityNodeInfo>()
        findAllNodes(root, allNodes)

        val metrics = Resources.getSystem().displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val centerX = screenWidth / 2
        val centerY = screenHeight / 2

        val dpi = metrics.densityDpi
        val distancePx = ((4f / 2.54f) * dpi).toInt() // ~2cm bán kính

        for (node in allNodes) {
            if (!node.isVisibleToUser) continue
            if (!node.isClickable) continue

            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val nodeCenterX = (bounds.left + bounds.right) / 2
            val nodeCenterY = (bounds.top + bounds.bottom) / 2

            val dx = nodeCenterX - centerX
            val dy = nodeCenterY - centerY
            val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toInt()

            if (distance <= distancePx) {
                return node
            }
        }
        return null
    }


    private fun findCloseButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodeList = ArrayList<AccessibilityNodeInfo>()
        findAllNodes(root, nodeList)

        val metrics = Resources.getSystem().displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val dpi = metrics.densityDpi
        val minDistanceFromBottomPx = ((5f / 2.54f) * dpi).toInt()

        for (node in nodeList) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val height = bounds.height()
            val width = bounds.width()
            val centerX = (bounds.left + bounds.right) / 2

            if (height < 200 && width < 200) {
                val isCenterHorizontally = centerX in (screenWidth / 4)..(screenWidth * 3 / 4)
                val isFarFromBottom = bounds.bottom < (screenHeight - minDistanceFromBottomPx)
                val isInLowerHalf = bounds.top > screenHeight / 2

                if (isCenterHorizontally && isFarFromBottom && isInLowerHalf) {
                    if (node.className == "android.widget.Button" || node.className == "android.widget.ImageView") {
                        return node
                    }
                }
            }
        }
        return null
    }

    private fun findAllNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        list.add(node)
        for (i in 0 until node.childCount) {
            findAllNodes(node.getChild(i), list)
        }
    }

    private fun getGoButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val countdownNodes = mutableListOf<AccessibilityNodeInfo>()
        collectCountdownNodes(root, countdownNodes)

        if (countdownNodes.isEmpty()) return null

        val lastCountdown = countdownNodes.last()

        if (countdownNodes.size == 3 || (countdownNodes.size == 2 && findNodeByText(root, "xu streamer") == null)) {
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

    private fun performScrollOrSwipe() {
        val scrolled = performGlobalAction(4096)
        if (!scrolled) {
            swipeManually()
        }
    }

    private fun swipeManually() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val startX = (screenWidth / 2).toFloat()
        val startY = (screenHeight * 0.7f)
        val endY = (screenHeight * 0.4f)

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // 🧩 Hàm tìm text theo yêu cầu mới: không phân biệt hoa thường, chỉ cần chứa
    private fun findNodeByText(node: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val text = node.text?.toString()?.trim()?.lowercase()
        if (text != null && text.contains(keyword.lowercase())) {
            return node
        }
        for (i in 0 until node.childCount) {
            val result = findNodeByText(node.getChild(i), keyword)
            if (result != null) return result
        }
        return null
    }
}
