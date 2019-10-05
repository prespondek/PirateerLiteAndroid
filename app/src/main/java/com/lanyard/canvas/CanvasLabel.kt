/*
 * Copyright 2019 Peter Respondek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lanyard.canvas

import android.graphics.*
import com.lanyard.helpers.intersects

class CanvasLabel : CanvasNode {

    override var magnitude : Size
    get() {
        if (_dirty) {
            updateSize()
        }
        return field
    }
    var fontAsset : String?
    set (value) {
        field =     value
        if (value != null) {
            typeface = Typeface.createFromAsset(view!!.context.assets, field);
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
        set(value) {
            field = value
            _dirty = true
        }
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
        magnitude =          Size(0,0)
    }
    constructor(text: String, font: String?) : super() {
        this.text = text
        this.fontAsset = font
    }

    override fun bounds(transform: CanvasNodeTransform) : Rect
    {
        var left = (transform.position.x + position.x - magnitude.width * anchor.x).toInt()
        var top = (transform.position.y + position.y - magnitude.height * (1-anchor.y)).toInt()
        var right = (left + magnitude.width).toInt()
        var bottom = (top + magnitude.height).toInt()
        return Rect(left,top,right,bottom)
    }

    fun updateSize () {
        val paint = Paint()
        paint.typeface = typeface
        paint.textSize = fontSize
        paint.style = fontStyle
        paint.strokeWidth = strokeWidth
        var bounds = Rect(0,0,0,0)
        paint.getTextBounds(text, 0, text.length, bounds);
        magnitude = Size(
            bounds.right - bounds.left,
            bounds.bottom - bounds.top)
        _dirty = false
    }

    override fun draw(canvas: Canvas, transform: CanvasNodeTransformData, view: Rect, timestamp: Long) {
        if (text.length > 0) {
            if (_dirty == true) {
                updateSize()
            }
            var bounds = bounds(transform)
            if (view.intersects(bounds)) {
                val paint = Paint()
                paint.setTypeface(typeface)
                paint.setTextSize(fontSize)
                paint.isAntiAlias = true
                if (fontStyle == Paint.Style.FILL_AND_STROKE) {
                    paint.strokeWidth = strokeWidth
                    paint.style = Paint.Style.STROKE
                    paint.color = strokeColor
                    paint.alpha = (transform.opacity * opacity * 255).toInt()
                    canvas.drawText(text, bounds.left.toFloat(), bounds.bottom.toFloat(), paint)
                    paint.style = Paint.Style.FILL
                } else {
                    paint.style = fontStyle
                }
                paint.color = fontColor
                paint.alpha = (transform.opacity * opacity * 255).toInt()
                canvas.drawText(text, bounds.left.toFloat(), bounds.bottom.toFloat(), paint)
            }
        }
        super.draw(canvas, transform,view, timestamp)
    }
}