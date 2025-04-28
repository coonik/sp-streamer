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

class MainService : AccessibilityService() {

    private var isAutoClicking = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        startAutoClicking()
    }

    override fun onDestroy() {
        super.onDestroy()
        isAutoClicking = false
    }

    private fun startAutoClicking() {
        clickLoop()
    }

    private fun clickLoop() {
        if (!isAutoClicking) return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val physicalScreenHeightCm = 14.5f
        val pixelPerCm = screenHeight / physicalScreenHeightCm

        val x = screenWidth / 2f
        val y = screenHeight - (8f * pixelPerCm)

        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50)) // nhanh
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                
                // Random delay 80–150ms cho tự nhiên
                val delay = (80..150).random().toLong()
                handler.postDelayed({ clickLoop() }, delay)

                // (Tùy chọn) Highlight click
                showHighlight(x, y)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                handler.postDelayed({ clickLoop() }, 300)
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
        }, 100) // Highlight 100ms rồi biến mất
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
