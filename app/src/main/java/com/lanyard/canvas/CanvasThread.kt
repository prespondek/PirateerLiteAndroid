package com.lanyard.canvas

import android.graphics.Canvas
import android.view.SurfaceHolder
import kotlin.math.max

/**
 * Created by arjun on 26/12/17.
 */

class CanvasThread(private val surfaceHolder: SurfaceHolder, private val gameView: CanvasView) : Thread() {
    private var running: Boolean = false

    private val targetFPS = 60 // frames per second, the rate at which you would like to refresh the Canvas
    private var canvas: Canvas? = null

    fun setRunning(isRunning: Boolean) {
        this.running = isRunning
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
                        BitmapCache.instance.flushExpired(System.currentTimeMillis() - 1000)
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
