package com.streamerbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityEvent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import android.content.res.Resources


class MainService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        startAutoClicking()
    }

    override fun onDestroy() {
        super.onDestroy()
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
                Thread.sleep(500)
                clickByPosition(2.5f)
                clickByPosition(2.75f)
                clickByPosition(3f)
                clickByPosition(2.5f)
                clickByPosition(2.75f)
                clickByPosition(3f)
                Thread.sleep(500)

                val goButton = getGoButton(root)
                if (goButton != null) {
                    findClickableNodeByText(root, "Theo dõi", true)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    goButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Thread.sleep(5000)
                    continue
                }

                val xuStreamer = findClickableNodeByText(root, "xu streamer")
                if (xuStreamer != null) {
                    findClickableNodeByText(root, "lưu")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

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
                Thread.sleep(2000)
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
        val baseY = screenHeight - (8f * pixelPerCm) // Vị trí nút quay thưởng
        val y = baseY + (offsetFromSpinButtonCm * pixelPerCm) // Thêm offset nếu có

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

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
            }
        }, null)
    }

    private fun showHighlight(x: Float, y: Float) {
        val highlightView = HighlightView(this)
        highlightView.setHighlight(x, y, 50f) // 50px bán kính

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(highlightView, params)

        Handler(Looper.getMainLooper()).postDelayed({
            highlightView.clearHighlight()
            windowManager.removeView(highlightView)
        }, 500) // Highlight 100ms rồi biến mất
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

        if (countdownNodes.isEmpty()) {
            return null
        }

        val lastCountdown = countdownNodes.last()

        if (countdownNodes.size == 3 || (countdownNodes.size == 2 && findClickableNodeByText(root, "xu streamer") == null)) {
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

    private fun findText(root: AccessibilityNodeInfo, searchText: String): AccessibilityNodeInfo? {
        val nodeList = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(root, nodeList)

        // Duyệt qua danh sách node và tìm kiếm văn bản
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

class HighlightView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = 0x55FF0000 // đỏ nhạt
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private var highlightX = -1f
    private var highlightY = -1f
    private var highlightRadius = 0f

    fun setHighlight(x: Float, y: Float, radius: Float) {
        highlightX = x
        highlightY = y
        highlightRadius = radius
        invalidate()
    }

    fun clearHighlight() {
        highlightX = -1f
        highlightY = -1f
        highlightRadius = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (highlightX >= 0 && highlightY >= 0 && highlightRadius > 0) {
            canvas.drawCircle(highlightX, highlightY, highlightRadius, paint)
        }
    }
}
