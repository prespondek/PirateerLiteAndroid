package com.lanyard.canvas

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import com.lanyard.helpers.plus
import java.sql.Timestamp


class CanvasCustom (drawCall: (canvas: Canvas, node:CanvasNode, transform: CanvasNodeTransformData) -> Unit, size: Size): CanvasNode() {
    var customDraw : (canvas: Canvas, node:CanvasNode, transform: CanvasNodeTransformData) -> Unit

    override fun draw(canvas: Canvas, transform: CanvasNodeTransformData, view: Rect, timestamp: Long) {
        customDraw(canvas, this,transform.transformed(this))
        super.draw(canvas, transform, view, timestamp)
    }

    init {
        customDraw = drawCall
        this.magnitude = size
    }

}