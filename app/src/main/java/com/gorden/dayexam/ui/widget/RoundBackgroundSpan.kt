package com.gorden.dayexam.ui.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan

class RoundBackgroundSpan(private val bgColor: Int, val textColor: Int, private val borderColor: Int, val textSize: Float): ReplacementSpan() {

    override fun getSize(
        p0: Paint,
        p1: CharSequence?,
        p2: Int,
        p3: Int,
        p4: Paint.FontMetricsInt?
    ): Int {
        return p0.measureText(p1, p2, p3).toInt() + 20
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val color1 = paint.color
        paint.color = bgColor
        val rect = RectF(x + 2, (top + 5).toFloat(), x + paint.measureText(text, start, end).toInt() , (bottom - 5).toFloat())
        canvas.drawRoundRect(rect, 15f, 15f, paint)

        paint.color = borderColor
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rect, 15f, 15f, paint)

        paint.color = textColor

        paint.style = Paint.Style.FILL
        paint.textSize = textSize
        canvas.drawText(text, start, end, (x + 10), y.toFloat() - 3, paint)
        paint.color = color1
    }
}