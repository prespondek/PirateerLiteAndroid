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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

open class CanvasView(context: Context, attributes: AttributeSet? = null, defStyleAttr: Int = 0) : SurfaceView(context, attributes, defStyleAttr), SurfaceHolder.Callback {
    var scene : CanvasScene? = null
    set(value) {
        field = value
        value?.view = this
    }
    var canvasThread: CanvasThread? = null
    private set

    private var view : View? = null

    init {
        holder.addCallback(this);
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        canvasThread?.running = false
        canvasThread?.join()
        canvasThread = null
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        canvasThread = CanvasThread(holder, this)
        canvasThread?.running = true
        canvasThread?.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    fun update(dt: Long) {
        scene?.update(dt)
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        scene?.draw(canvas!!, Rect(0,0,width,height),System.currentTimeMillis())
    }
}