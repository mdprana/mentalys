package com.mentalys.app.ui.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceGuideOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(95, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }

    private val ovalRect = RectF()
    private val overlayPath = Path()
    private var faceDetected = false

    fun setFaceDetected(detected: Boolean) {
        if (faceDetected == detected) return
        faceDetected = detected
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateOvalRect()

        overlayPath.reset()
        overlayPath.fillType = Path.FillType.EVEN_ODD
        overlayPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        overlayPath.addOval(ovalRect, Path.Direction.CCW)
        canvas.drawPath(overlayPath, dimPaint)

        strokePaint.color = if (faceDetected) DETECTED_COLOR else IDLE_COLOR
        canvas.drawOval(ovalRect, strokePaint)
    }

    private fun updateOvalRect() {
        val ovalWidth = width * 0.66f
        val ovalHeight = height * 0.48f
        val centerX = width / 2f
        val centerY = height * 0.43f
        ovalRect.set(
            centerX - ovalWidth / 2f,
            centerY - ovalHeight / 2f,
            centerX + ovalWidth / 2f,
            centerY + ovalHeight / 2f
        )
    }

    companion object {
        private val IDLE_COLOR = Color.argb(235, 255, 255, 255)
        private val DETECTED_COLOR = Color.rgb(93, 222, 128)
    }
}
