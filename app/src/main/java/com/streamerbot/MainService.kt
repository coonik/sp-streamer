package com.streamerbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.res.Resources
import kotlinx.coroutines.*
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import android.os.Handler
import android.os.Looper

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
                clickRewardButton()
                Thread.sleep(3000)
            }
        }.start()
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

    private fun findClickableNodeByText(node: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (node == null) return null

        val text = node.text?.toString()?.trim()?.lowercase()
        if (text != null && text.contains(keyword.lowercase())) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isVisibleToUser) {
                return clickableNode
            }
        }

        for (i in 0 until node.childCount) {
            val result = findClickableNodeByText(node.getChild(i), keyword)
            if (result != null) return result
        }

        return null
    }

    private fun clickRewardButton() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Tính toán tọa độ của nút quay thưởng
        val x = (screenWidth / 2).toFloat() // Giữa màn hình
        val y = (screenHeight - (8 * screenHeight / 14.5f)).toFloat() // 8cm từ dưới lên

        val buttonRadius = (1.5f * screenWidth / 7.5f) // Đường kính nút khoảng 1.5cm, tỷ lệ với chiều rộng màn hình
        val startX = x
        val startY = y

        // Tạo gesture để click vào vị trí
        val path = Path().apply {
            moveTo(startX, startY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        // Thực hiện click
        dispatchGesture(gesture, null, null)

        // Tạo và thêm HighlightView vào màn hình
        val highlightView = HighlightView(this)
        highlightView.setHighlight(startX, startY, buttonRadius)

        // Thêm HighlightView vào cửa sổ chính
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(highlightView, params)

        // Sau 1 giây, xóa vùng highlight
        Handler(Looper.getMainLooper()).postDelayed({
            highlightView.clearHighlight()
            windowManager.removeView(highlightView)
        }, 1000)
    }

}
