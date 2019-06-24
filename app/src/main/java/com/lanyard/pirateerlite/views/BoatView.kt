package com.lanyard.pirateerlite.views

import android.graphics.PointF
import com.lanyard.canvas.CanvasActionCutom
import com.lanyard.canvas.CanvasSprite
import com.lanyard.library.SplinePath
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.canvas.BitmapCache
import com.lanyard.helpers.angle
import com.lanyard.helpers.minus
import com.lanyard.helpers.set
import java.util.*
import kotlin.math.PI
import kotlin.math.round


class BoatView(boatType: String) : SplinePath() {
    var         controller :    BoatController?
    private var icon :          String
    var         sprite :        CanvasSprite
    private var duration :      Long
    private var timer :         Long

    private val _boatType:      String
    val boatType: String
        get() {
            return _boatType
        }

    init {
        this.controller =   null
        this.duration =     0
        this.timer =        0
        _boatType =         boatType
        this.icon =         boatType + "_01.png"
        val tex =           BitmapCache.instance.getBitmap(this.icon)
        this.sprite =       CanvasSprite(tex!!) //SKSpriteNode(texture: tex, color: UIColor.white, size: tex.size())
    }

    fun rotate (p0: PointF, p1: PointF) {
        val angle = ((p0 - p1).angle + PI) / (2.0 * PI / 16)
        println(p0.toString() + " " + p1.toString() + " " + (p1 - p0).angle.toString())
        var frame = round(angle)
        frame += 1
        if (frame > 16) {
            frame = 1.0
        }
        var texture_name = _boatType + "_"
        if (frame < 10) {
            texture_name += "0"
        }
        texture_name += frame.toInt().toString()
        if (this.icon != texture_name) {
            this.icon = texture_name
            sprite.texture = BitmapCache.instance.getBitmap(this.icon + ".png")!!
        }
    }


    fun sail (startTime: Date, duration: Long) {
        this.duration = duration * 1000
        this.timer = Date().time - startTime.time
        rotate(
            splinePosition(0.0f),
            splinePosition(Float.MIN_VALUE)
        )
        sprite.hidden = false

        smooth((length / 20.0).toInt())
        val action1 = CanvasActionCutom(this.duration, { node, dt ->
            val currTime = dt.toFloat() / this.duration
            val pos2 = this.splinePosition(currTime)
            this.sprite.position.set(pos2)
        })
        val action2 = CanvasActionCutom(this.duration, 500, { node, dt ->
            val currTime = dt.toFloat() / this.duration
            val nextTime =( dt.toFloat() + 500) / this.duration
            val pos1 = this.splinePosition(currTime)
            pos1.y = -pos1.y
            val pos2 = this.splinePosition(nextTime)
            pos2.y = -pos2.y
            rotate(pos2,pos1)
        })
        action2.action(this.sprite,0)
        this.sprite.run(action1)
        this.sprite.run(action2)

    }

}