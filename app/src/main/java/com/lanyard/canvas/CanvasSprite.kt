package com.lanyard.canvas

import android.graphics.*
import androidx.core.graphics.ColorUtils
import com.lanyard.helpers.ColorHelper
import com.lanyard.helpers.intersects
import com.lanyard.helpers.plus


open class CanvasSprite : CanvasNode {
    var texture : BitmapStream

    init {

    }

    constructor(bitmap : BitmapStream) : super() {
        texture = bitmap
        this.magnitude = Size(texture.width,texture.height)
    }

    override fun draw(canvas: Canvas, transform: CanvasNodeTransformData, view: Rect, timestamp: Long) {
        var bounds = bounds(transform)
        if (view.intersects(bounds)) {
            var paint = Paint()
            paint.alpha = (transform.opacity * opacity * 255).toInt()
            canvas.drawBitmap(texture.getBitmap(timestamp), null, bounds, paint)
        }
        super.draw(canvas,transform,view, timestamp)
    }

}