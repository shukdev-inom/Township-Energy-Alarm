package com.shukdev.townshipenergy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Circular energy ring: cream disc, tan track, green progress arc, wooden outer rim. */
class EnergyRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var progress: Float = 0f
        set(value) {
            val v = value.coerceIn(0f, 1f)
            if (v != field) {
                field = v
                invalidate()
            }
        }

    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFF6DC.toInt()
    }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFE2C58A.toInt()
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = 0xFF5CC13A.toInt()
    }
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFA8672D.toInt()
    }
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height).toFloat()
        val f = size / 140f
        val cx = width / 2f
        val cy = height / 2f
        val r = 56f * f

        track.strokeWidth = 14f * f
        arc.strokeWidth = 14f * f
        rim.strokeWidth = 3f * f

        canvas.drawCircle(cx, cy, r, disc)
        canvas.drawCircle(cx, cy, r, track)
        if (progress > 0f) {
            oval.set(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(oval, -90f, 360f * progress, false, arc)
        }
        canvas.drawCircle(cx, cy, 63f * f, rim)
    }
}
