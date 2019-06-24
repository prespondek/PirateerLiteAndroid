package com.lanyard.canvas

import android.graphics.Canvas
import android.graphics.Point

class CanvasScene : CanvasNode () {
    fun draw(canvas: Canvas) {
        draw(canvas, Point(0,0))
    }
}