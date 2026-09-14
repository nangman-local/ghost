package com.ghost.prototype.floating

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.ghost.prototype.R

/** Temporary code-drawn character. No interaction policy or window ownership here. */
class CharacterView(context: Context) : View(context) {
    var greetingVisible: Boolean = false
        set(value) {
            field = value
            contentDescription = context.getString(
                if (value) R.string.ghost_greeting_description else R.string.ghost_description,
            )
            invalidate()
        }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body = Path().apply {
        moveTo(10f, 72f)
        lineTo(10f, 37f)
        cubicTo(10f, 0f, 70f, 0f, 70f, 37f)
        lineTo(70f, 72f)
        quadTo(65f, 83f, 58f, 72f)
        quadTo(49f, 86f, 40f, 73f)
        quadTo(31f, 86f, 22f, 72f)
        quadTo(15f, 83f, 10f, 72f)
        close()
    }

    init {
        contentDescription = context.getString(R.string.ghost_description)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(width / 80f, width / 80f)
        if (greetingVisible) {
            drawGreeting(canvas)
            canvas.translate(0f, GREETING_HEIGHT_DP)
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(246, 242, 255)
        canvas.drawPath(body, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.rgb(99, 84, 181)
        canvas.drawPath(body, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(48, 39, 77)
        canvas.drawOval(27f, 34f, 33f, 44f, paint)
        canvas.drawOval(47f, 34f, 53f, 44f, paint)
        paint.color = Color.rgb(236, 190, 207)
        canvas.drawOval(19f, 46f, 30f, 51f, paint)
        canvas.drawOval(50f, 46f, 61f, 51f, paint)
        canvas.restore()
    }

    private fun drawGreeting(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(246, 242, 255)
        canvas.drawRoundRect(1.5f, 1.5f, 78.5f, 31f, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.rgb(99, 84, 181)
        canvas.drawRoundRect(1.5f, 1.5f, 78.5f, 31f, 10f, 10f, paint)
        canvas.drawLine(36f, 31f, 40f, 36f, paint)
        canvas.drawLine(40f, 36f, 44f, 31f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(48, 39, 77)
        paint.textSize = 14f * resources.configuration.fontScale.coerceAtMost(1.5f)
        paint.textAlign = Paint.Align.CENTER
        val metrics = paint.fontMetrics
        canvas.drawText(context.getString(R.string.greeting), 40f, 16f - (metrics.ascent + metrics.descent) / 2f, paint)
    }

    companion object {
        const val GREETING_HEIGHT_DP = 38f
    }
}
