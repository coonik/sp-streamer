package com.streamerbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MainService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var currentHighlightView: HighlightView? = null
    private var currentHighlightVisible = false
    private var isNeedToClose = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        startAutoClicking()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeHighlight()
    }

    private fun startAutoClicking() {
        Thread {
            while (true) {
                val root = rootInActiveWindow ?: continue
                root.refresh()

                if (findText(root, "Vòng Quay") != null) {
                    isNeedToClose = true
                    clickByPosition()
                    Thread.sleep(50)
                    continue
                }

                if (isNeedToClose) {
                    Thread.sleep(500)
                    listOf(2f, 2.25f, 2.5f, 2.75f, 3f).forEach { offset ->
                        clickByPosition(offset)
                    }
                    Thread.sleep(500)
                    isNeedToClose = false
                }

                val goButton = getGoButton(root)
                if (goButton != null) {
                    findClickableNodeByText(root, "Theo dõi", true)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    val countdownText = goButton.text?.toString() ?: ""
                    if (extractMinutes(countdownText) <= 1) {
                        goButton.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Thread.sleep(5000)
                        continue
                    }
                }

                val xuStreamer = findClickableNodeByText(root, "xu streamer")
                if (xuStreamer != null) {
                    val countdownText = findCountdownNear(xuStreamer)
                    if (countdownText != null && extractMinutes(countdownText) <= 5) {
                        Thread.sleep(1000)
                        continue
                    } else {
                        repeat(2) {
                            findClickableNodeByText(root, "lưu")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                        Thread.sleep(1000)
                        continue
                    }
                }

                performScrollOrSwipe()
                Thread.sleep(3500)
            }
        }.start()
    }

    private fun clickByPosition(offsetFromSpinButtonCm: Float = 0f) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val physicalScreenHeightCm = 14.5f
        val pixelPerCm = screenHeight / physicalScreenHeightCm

        val x = screenWidth / 2f
        val baseY = screenHeight - (8f * pixelPerCm)
        val y = baseY + (offsetFromSpinButtonCm * pixelPerCm)

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                showHighlight(x, y)
            }
        }, null)
    }

    private fun showHighlight(x: Float, y: Float) {
        if (currentHighlightVisible) return

        val highlightView = HighlightView(this).apply {
            setHighlight(x, y, 50f)
        }

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(highlightView, params)
        currentHighlightView = highlightView
        currentHighlightVisible = true

        handler.postDelayed({ removeHighlight() }, 100)
    }

    private fun removeHighlight() {
        currentHighlightView?.let {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
            currentHighlightView = null
            currentHighlightVisible = false
        }
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

        val totalSubCountdown = listOf("Điểm danh 7 ngày", "Xem live").sumOf { findSubCountdown(root, it) }

        val xuStreamerExists = findClickableNodeByText(root, "xu streamer") != null

        return if (countdownNodes.size == (2 + totalSubCountdown) ||
            (countdownNodes.size == (1 + totalSubCountdown) && !xuStreamerExists)
        ) {
            lastCountdown
        } else null
    }

    private fun findSubCountdown(root: AccessibilityNodeInfo, keyword: String): Int {
        val node = findClickableNodeByText(root, keyword)
        return if (node != null && findCountdownNear(node) != null) 1 else 0
    }

    private fun collectCountdownNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        node.text?.toString()?.takeIf { it.matches(Regex("\\d{1,2}:\\d{2}")) }?.let {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            collectCountdownNodes(node.getChild(i), list)
        }
    }

    private fun findCountdownNear(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        for (i in 0 until parent.childCount) {
            val text = parent.getChild(i)?.text?.toString()
            if (text != null && text.matches(Regex("\\d{1,2}:\\d{2}"))) {
                return text
            }
        }
        return null
    }

    private fun extractMinutes(countdown: String): Int {
        return countdown.split(":").getOrNull(0)?.toIntOrNull() ?: 0
    }

    private fun performScrollOrSwipe() {
        val scrolled = performGlobalAction(AccessibilityService.GLOBAL_ACTION_SCROLL_FORWARD)
        if (!scrolled) swipeManually()
    }

    private fun swipeManually() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val startX = screenWidth / 2f
        val startY = screenHeight * 0.7f
        val endY = screenHeight * 0.4f

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun findText(root: AccessibilityNodeInfo, searchText: String): AccessibilityNodeInfo? {
        val nodeList = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(root, nodeList)
        return nodeList.firstOrNull {
            it.text?.toString()?.trim()?.equals(searchText, ignoreCase = true) == true
        }
    }

    private fun findClickableNodeByText(node: AccessibilityNodeInfo?, keyword: String, exactMatch: Boolean = false): AccessibilityNodeInfo? {
        if (node == null) return null

        val text = node.text?.toString()?.trim()
        val match = if (exactMatch) {
            text == keyword
        } else {
            text?.lowercase()?.contains(keyword.lowercase()) == true
        }

        if (match) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isVisibleToUser) {
                return clickableNode
            }
        }

        for (i in 0 until node.childCount) {
            val result = findClickableNodeByText(node.getChild(i), keyword, exactMatch)
            if (result != null) return result
        }

        return null
    }
}
