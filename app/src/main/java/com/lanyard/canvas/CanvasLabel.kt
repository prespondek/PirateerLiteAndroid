package com.lanyard.canvas

import android.graphics.*
import android.util.Size
import com.lanyard.helpers.plus
import com.lanyard.pirateerlite.MapActivity

class CanvasLabel : CanvasNode {


    var fontAsset : String?
    set (value) {
        field =     value
        if (value != null) {
            typeface = Typeface.createFromAsset(MapActivity.instance.assets, field);
        }
    }
    var text :          String
    set(value) {
        field = value
        _dirty = true
    }
    var fontColor :     Int
    var fontSize :      Float
        set(value) {
            field = value
            _dirty = true
        }
    var fontStyle :     Paint.Style
        set(value) {
            field = value
            _dirty = true
        }
    var strokeWidth :   Float
    var strokeColor :   Int
    var typeface :      Typeface
        set(value) {
            field = value
            _dirty = true
        }
    private var _dirty : Boolean = false
    init {
        fontColor =     Color.WHITE
        strokeColor =   Color.BLACK
        fontSize =      8.0f
        strokeWidth =   1.0f
        fontStyle =     Paint.Style.FILL
        typeface =      Typeface.DEFAULT
        text =          ""
    }
    constructor(text: String, font: String?) : super() {
        this.text = text
        this.fontAsset = font
    }

    override fun bounds(pos: Point) : Rect
    {
        var left = (pos.x + position.x - size.width * scale.width * anchor.x).toInt()
        var top = (pos.y + position.y + size.height * scale.height * anchor.y).toInt()
        var right = (left + size.width * scale.width).toInt()
        var bottom = (top + size.height * scale.height).toInt()
        return Rect(left,top,right,bottom)
    }

    fun updateSize () {
        val paint = Paint()
        paint.typeface = typeface
        paint.textSize = fontSize
        paint.style = fontStyle
        var bounds = Rect(0,0,0,0)
        paint.getTextBounds(text, 0, text.length, bounds);
        size = Size(bounds.right - bounds.left, bounds.bottom - bounds.top)
        _dirty = false
    }

    override fun draw(canvas: Canvas, pos: Point) {
        if ( _dirty == true ) { updateSize() }
        val paint = Paint()
        paint.setTypeface(typeface)
        paint.setTextSize(fontSize)
        var bounds = bounds(pos)
        if ( fontStyle == Paint.Style.FILL_AND_STROKE ) {
            paint.strokeWidth = strokeWidth
            paint.style = Paint.Style.STROKE
            paint.color = strokeColor
            canvas.drawText(text, bounds.left.toFloat(), bounds.top.toFloat(), paint)
            paint.style = Paint.Style.FILL
        } else {
            paint.style = fontStyle
        }
        paint.color = fontColor
        canvas.drawText(text, bounds.left.toFloat(), bounds.top.toFloat(), paint)
        super.draw(canvas, pos + position)
    }
}