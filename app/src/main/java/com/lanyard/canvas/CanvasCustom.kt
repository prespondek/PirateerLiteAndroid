package com.lanyard.canvas

import android.graphics.Canvas
import android.graphics.Point
import android.util.Size
import com.lanyard.helpers.plus


class CanvasCustom (drawCall: (canvas: Canvas, node:CanvasNode, pos: Point) -> Unit, size: Size): CanvasNode() {
    var customDraw : (canvas: Canvas, node:CanvasNode, pos: Point) -> Unit

    override fun draw(canvas: Canvas, pos: Point) {
        customDraw(canvas, this,pos + position)
        super.draw(canvas, pos)
    }

    init {
        customDraw = drawCall
        this.size = size
    }

}