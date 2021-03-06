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
import kotlin.math.min
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
        this.duration = duration
        this.timer = Date().time - startTime.time
        rotate(
            splinePosition(0.0f),
            splinePosition(Float.MIN_VALUE)
        )
        sprite.hidden = false

        smooth((length / 20.0).toInt())
        val action1 = CanvasActionCutom(this.duration, { node, dt ->
            val currTime = min(1.0f,(Date().time - startTime.time).toFloat() / duration)
            val nextTime = min(1.0f, (currTime * this.duration + 500) / this.duration)
            val pos = this.splinePosition(currTime)
            this.sprite.position.set(pos)
            var pos1 = PointF(pos.x,pos.y)
            pos1.y = -pos1.y
            val pos2 = this.splinePosition(nextTime)
            pos2.y = -pos2.y
            rotate(pos2,pos1)
        })
        action1.tag = "sail"
        action1.action(this.sprite,0.0f)
        this.sprite.run(action1)
    }

}