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
import android.view.SurfaceHolder
import kotlin.math.max

class CanvasThread(private val surfaceHolder: SurfaceHolder, private val gameView: CanvasView) : Thread() {
    private val targetFPS = 60 // frames per second, the rate at which you would like to refresh the Canvas
    private var canvas: Canvas? = null
    var running : Boolean = false
    set(value) {
        field = value
    }

    override fun run() {

        var startTime:  Long
        var timeMillis: Long = 0
        var waitTime:   Long
        val targetTime = (1000 / targetFPS).toLong()

        while (running) {
            startTime = System.nanoTime()
            canvas = null

            try {
                // locking the canvas allows us to draw on to it
                canvas = this.surfaceHolder.lockCanvas()
                    synchronized(surfaceHolder) {
                        this.gameView.postInvalidate()
                        this.gameView.update(timeMillis)
                        this.gameView.draw(canvas!!)
                        BitmapCache.instance.flushExpired(System.currentTimeMillis())
                    }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            timeMillis = (System.nanoTime() - startTime) / 1000000
            waitTime = targetTime - timeMillis
            try {
                sleep(max(0,waitTime))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            timeMillis = (System.nanoTime() - startTime) / 1000000
        }
    }

}
