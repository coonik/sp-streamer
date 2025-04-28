package com.streamerbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class HighlightView(context: Context) : View(context) {
    private var highlightX: Float = -1f
    private var highlightY: Float = -1f
    private var highlightRadius: Float = 0f
    private val paint = Paint().apply {
        color = Color.argb(100, 255, 255, 0) // Màu vàng trong suốt
        style = Paint.Style.FILL
        isAntiAlias = true
    }

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
        if (highlightRadius > 0f) {
            canvas.drawCircle(highlightX, highlightY, highlightRadius, paint)
        }
    }
}
