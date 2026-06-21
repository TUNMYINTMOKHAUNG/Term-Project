package com.example.myapplication.ui.upload

import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.PixelFormat

class PatternCircleDrawable(private val pattern: String) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val rect = RectF(bounds)

        val circlePath = Path().apply {
            addOval(rect, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(circlePath)

        // Inside PatternCircleDrawable.kt -> draw()
// Use lowercase and trim to ensure it matches no matter how it was saved
        val normalizedPattern = pattern?.lowercase()?.trim() ?: "plain"

        when (normalizedPattern) {
            "plain" -> drawPlain(canvas, rect)
            "stripes", "striped" -> drawStripes(canvas, rect) // Added "striped" as a fallback
            "dotted" -> drawDotted(canvas, rect)
            "checkered" -> drawCheckered(canvas, rect)
            "floral" -> drawFloral(canvas, rect)
            else -> drawOthers(canvas, rect)
        }

        canvas.restore()

        // Inside draw(canvas: Canvas)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f // Make it slightly thicker to match MaterialCardView borders
        paint.color = Color.parseColor("#CCCCCC")
        canvas.drawOval(rect, paint) // This draws the circle border
    }

    // --- Drawing Methods ---
    private fun drawPlain(canvas: Canvas, rect: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#EAEAEA")
        canvas.drawRect(rect, paint)
    }

    private fun drawStripes(canvas: Canvas, rect: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRect(rect, paint)

        paint.color = Color.parseColor("#222222")
        val stripeWidth = rect.width() / 7f

        var x = rect.left - stripeWidth
        while (x < rect.right + stripeWidth) {
            canvas.drawRect(x, rect.top, x + stripeWidth / 2f, rect.bottom, paint)
            x += stripeWidth
        }
    }

    private fun drawDotted(canvas: Canvas, rect: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRect(rect, paint)
        paint.color = Color.parseColor("#222222")
        val spacing = rect.width() / 4f
        val dotRadius = rect.width() / 14f
        var y = rect.top + spacing / 2f
        var row = 0
        while (y < rect.bottom) {
            var x = rect.left + spacing / 2f
            if (row % 2 != 0) x += spacing / 2f
            while (x < rect.right) {
                canvas.drawCircle(x, y, dotRadius, paint)
                x += spacing
            }
            row++; y += spacing
        }
    }

    private fun drawCheckered(canvas: Canvas, rect: RectF) {
        val cells = 4
        val cellW = rect.width() / cells
        val cellH = rect.height() / cells
        for (y in 0 until cells) {
            for (x in 0 until cells) {
                paint.style = Paint.Style.FILL
                paint.color = if ((x + y) % 2 == 0) Color.WHITE else Color.parseColor("#2F5597")
                canvas.drawRect(rect.left + x * cellW, rect.top + y * cellH,
                    rect.left + (x + 1) * cellW, rect.top + (y + 1) * cellH, paint)
            }
        }
    }

    private fun drawFloral(canvas: Canvas, rect: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FFF1F3")
        canvas.drawRect(rect, paint)
        drawFlower(canvas, rect.centerX(), rect.centerY(), rect.width() * 0.13f)
        drawFlower(canvas, rect.left + rect.width() * 0.30f, rect.top + rect.height() * 0.30f, rect.width() * 0.09f)
        drawFlower(canvas, rect.left + rect.width() * 0.72f, rect.top + rect.height() * 0.72f, rect.width() * 0.10f)
    }

    private fun drawFlower(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E91E63")
        canvas.drawCircle(cx, cy - size, size, paint)
        canvas.drawCircle(cx + size, cy, size, paint)
        canvas.drawCircle(cx, cy + size, size, paint)
        canvas.drawCircle(cx - size, cy, size, paint)
        paint.color = Color.parseColor("#FFD54F")
        canvas.drawCircle(cx, cy, size * 0.65f, paint)
    }

    private fun drawOthers(canvas: Canvas, rect: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#D6D6D6")
        canvas.drawRect(rect, paint)
    }

    // --- Required Drawable Overrides ---
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT


}

