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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.lanyard.helpers.intersects

open class CanvasSprite(bitmap: BitmapStream) : CanvasNode() {
    var texture: BitmapStream = bitmap

    init {

    }

    init {
        magnitude.set(texture.width, texture.height)
    }

    override fun draw(canvas: Canvas, transform: CanvasNodeTransformData, view: Rect, timestamp: Long) {
        val bounds = bounds(transform)
        if (view.intersects(bounds)) {
            val paint = Paint()
            paint.alpha = (transform.opacity * opacity * 255).toInt()
            canvas.drawBitmap(texture.getBitmap(timestamp), null, bounds, paint)
        }
        super.draw(canvas,transform,view, timestamp)
    }

}