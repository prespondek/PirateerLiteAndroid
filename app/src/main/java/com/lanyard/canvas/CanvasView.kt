package com.lanyard.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import androidx.cardview.widget.CardView
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
        canvasThread = CanvasThread(holder, this)
    }


    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        var retry = true
        while (retry) {
            try {
                this.canvasThread?.setRunning(false)
                this.canvasThread?.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                retry = false
            }
        }
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        setWillNotDraw(false)
        this.canvasThread?.setRunning(true)
        this.canvasThread?.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    /**
     * Function to update the positions of player and game objects
     */
    fun update(dt: Long) {
        scene?.update(dt)
    }


    /**
     * Everything that has to be drawn on Canvas
     */

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        scene?.draw(canvas!!, Rect(0,0,width,height),System.currentTimeMillis())
    }
}