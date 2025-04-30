package com.streamerbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import android.content.res.Resources
import android.util.Log


class MainService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var currentHighlightView: HighlightView? = null
    private var currentHighlightVisible = false

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

                val popup = findText(root, "Vòng Quay")
                if (popup != null) {
                    clickByPosition()
                    Thread.sleep(50)
                    continue
                }

                val isNeedToClose = findClickableNodeByText(root, "Xem thành tích của người chơi khác")
                if (isNeedToClose != null) {
                    val closeBtn = findCloseButton(root)
                    Log.d("MainService", "Click close button: $closeBtn")
                    closeBtn?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                    Thread.sleep(50)
                    continue
                }

                var quayMinutes = 5
                val goButton = getGoButton(root)
                if (goButton != null) {
                    findClickableNodeByText(root, "Theo dõi", true)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                    val quayCountdownText = goButton.text?.toString() ?: ""
                    quayMinutes = extractMinutes(quayCountdownText)
                    if (quayMinutes <= 1) {
                        goButton.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Thread.sleep(5000)
                        continue
                    }
                }

                val xuStreamer = findClickableNodeByText(root, "xu streamer")
                if (xuStreamer != null) {
                    val countdownText = findCountdownNear(xuStreamer)
                    if (countdownText != null) {
                        val minutes = extractMinutes(countdownText)
                        if (minutes <= 5) {
                            Thread.sleep(1000)
                            continue
                        }
                    } else {
                        findClickableNodeByText(root, "lưu")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        findClickableNodeByText(root, "lưu")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Thread.sleep(1000)
                        continue
                    }
                }
                if (goButton != null && quayMinutes <= 5) {
                    continue
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

        val path = Path().apply {
            moveTo(x, y)
        }
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
    
    private fun findActiveTab(root: AccessibilityNodeInfo): String? {
        val nodeList = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(root, nodeList)

        for (node in nodeList) {
            // Kiểm tra xem tab có được chọn hay không
            if (node.className == "android.widget.TextView" && node.isSelected) {
                val text = node.text?.toString()
                if (text != null) {
                    if (text.contains("Video", ignoreCase = true)) {
                        return "Video"
                    }
                    if (text.contains("Live", ignoreCase = true)) {
                        return "Live"
                    }
                }
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

        // Đổi cm sang pixels
        val centerX = screenWidth / 2
        val centerY = screenHeight - ((4f / 2.54f) * dpi).toInt()

        val halfWidth = ((1f / 2.54f) * dpi).toInt() // 1cm mỗi bên
        val halfHeight = ((1.5f / 2.54f) * dpi).toInt() // 1.5cm mỗi bên

        val targetRect = Rect(
            centerX - halfWidth,
            centerY - halfHeight,
            centerX + halfWidth,
            centerY + halfHeight
        )

        for (node in nodeList) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val isInTargetZone = Rect.intersects(bounds, targetRect)
            val isSmall = bounds.width() < 200 && bounds.height() < 200

            if (isInTargetZone && isSmall) {
                if ((node.className == "android.widget.ImageView" || node.className == "android.widget.Button")
                    && node.isVisibleToUser && node.isEnabled
                ) {
                    return node
                }
            }
        }

        return null
    }



    private fun showHighlight(x: Float, y: Float) {
        if (currentHighlightVisible) return

        val highlightView = HighlightView(this)
        highlightView.setHighlight(x, y, 50f)

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

        Handler(Looper.getMainLooper()).postDelayed({
            removeHighlight()
        }, 100)
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

    private fun findSubCountdown(root: AccessibilityNodeInfo, keyword: String): Int {
        val node = findText(root, keyword)
        if (node != null) {
            val countdownText = findCountdownNear(node) ?: findCountdownNear(node.parent)
            if (countdownText != null) {
                return 1
            }
        }
        return 0
    }

    private fun getGoButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val countdownNodes = mutableListOf<AccessibilityNodeInfo>()
        collectCountdownNodes(root, countdownNodes)

        if (countdownNodes.isEmpty()) return null

        if (countdownNodes.size == 1 && findClickableNodeByText(root, "Top nhà sáng tạo") == null) return null
        val lastCountdown = countdownNodes.last()
        val totalSubCoundown = findSubCountdown(root, "Điểm danh 7 ngày") + findSubCountdown(root, "Xem Live")

        return if (countdownNodes.size == (2 + totalSubCoundown) || (countdownNodes.size == (1 + totalSubCoundown) && findClickableNodeByText(root, "xu streamer") == null)) {
            lastCountdown
        } else null
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

    private fun findText(root: AccessibilityNodeInfo, searchText: String): AccessibilityNodeInfo? {
        val nodeList = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(root, nodeList)
        for (node in nodeList) {
            val text = node.text?.toString()?.trim()
            if (text != null && text.equals(searchText, ignoreCase = true)) {
                return node
            }
        }
        return null
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