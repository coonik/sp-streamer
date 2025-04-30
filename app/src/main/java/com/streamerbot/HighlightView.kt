package com.streamerbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class HighlightView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = 0x99FF0000.toInt()
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (highlightX >= 0 && highlightY >= 0 && highlightRadius > 0) {
            canvas.drawCircle(highlightX, highlightY, highlightRadius, paint)
        }
    }
}