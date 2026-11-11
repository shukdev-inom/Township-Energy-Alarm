package com.shukdev.townshipenergy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/** TextView that draws a cartoon outline around its text (like in-game lettering). */
class OutlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var outlineColor = Color.BLACK
    private var outlineWidthPx = 0f
    private var drawing = false

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.OutlinedTextView)
        try {
            outlineColor = a.getColor(R.styleable.OutlinedTextView_otvColor, Color.BLACK)
            outlineWidthPx = a.getDimension(R.styleable.OutlinedTextView_otvWidth, 0f)
        } finally {
            a.recycle()
        }
    }

    // setTextColor() calls invalidate(); ignore it while we are inside onDraw to avoid a draw loop.
    override fun invalidate() {
        if (!drawing) super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (outlineWidthPx <= 0f) {
            super.onDraw(canvas)
            return
        }
        drawing = true
        val fillColors = textColors
        val p = paint
        p.style = Paint.Style.STROKE
        p.strokeWidth = outlineWidthPx
        p.strokeJoin = Paint.Join.ROUND
        setTextColor(outlineColor)
        super.onDraw(canvas)
        p.style = Paint.Style.FILL
        setTextColor(fillColors)
        super.onDraw(canvas)
        drawing = false
    }
}
