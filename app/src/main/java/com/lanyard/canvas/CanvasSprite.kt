package com.lanyard.canvas

import android.graphics.Canvas
import android.graphics.Point
import android.util.Size
import com.lanyard.helpers.plus


open class CanvasSprite : CanvasNode {
    var texture : BitmapStream

    init {

    }


    constructor(bitmap : BitmapStream) : super() {
        texture = bitmap
        this.size = Size(texture.width,texture.height)
    }

    override fun draw(canvas: Canvas, pos: Point) {
        var bounds = bounds(pos)
        canvas.drawBitmap(texture.bitmap, null, bounds, null)
        super.draw(canvas,pos + position)
    }

}