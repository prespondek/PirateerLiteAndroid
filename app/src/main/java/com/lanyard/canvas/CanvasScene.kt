package com.lanyard.canvas

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect

class CanvasScene : CanvasNode () {

    fun draw(canvas: Canvas, view: Rect, timestamp: Long) {
        draw(canvas, CanvasNodeTransformData(), view, timestamp)
    }

}