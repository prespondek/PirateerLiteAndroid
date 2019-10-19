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
import android.graphics.Rect

class CanvasCustom (drawCall: (canvas: Canvas, node:CanvasNode, transform: CanvasNodeTransformData) -> Unit, size: Size): CanvasNode() {
    var customDraw : (canvas: Canvas, node:CanvasNode, transform: CanvasNodeTransformData) -> Unit

    override fun draw(canvas: Canvas, transform: CanvasNodeTransformData, view: Rect, timestamp: Long) {
        customDraw(canvas, this,transform.transformed(this))
        super.draw(canvas, transform, view, timestamp)
    }

    init {
        customDraw = drawCall
        this.magnitude.set(size)
    }

}