package com.example.lockar.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View


class DrawView : View {
    private var changingType: Boolean = false
    private var paint: Paint = Paint()
    private var path: Path = Path()
    private var rectangle: Drawable? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context,
        attrs,
        defStyleAttr) {
        paint.color = Color.argb(50, 206, 58, 147)
        paint.strokeWidth = 10f
        paint.style = Paint.Style.FILL_AND_STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        this.path.let {
            canvas.drawPath(it, paint)
        }
    }

    private fun drawRectangle(canvas: Canvas) {
        val imageBounds: Rect = canvas.clipBounds
        rectangle!!.bounds = imageBounds
        rectangle!!.draw(canvas)
    }

    fun setPath(path: Path) {
        this.path = path
    }

    private fun clearCanvas(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    fun setRectangle(value: Boolean ) {
        this.invalidate();
        this.changingType = value;

    }
}