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

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import com.lanyard.canvas.CanvasNode
import com.lanyard.helpers.plus
import com.lanyard.helpers.unaryMinus
import com.lanyard.library.SuperScrollView

class MapScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : SuperScrollView(context, attrs, defStyleAttr)
{
    var target : Point? = null
    var scene : MapView? = null
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        var curr_target = target
        if (curr_target != null && scene != null) {
            var pos = screenPosition(curr_target)
            scrollTo(pos.x,pos.y)
            scene!!.position = -Point(pos.x, pos.y) + scene!!.padding
            target = null
        }
    }

    fun boatTracker(node: CanvasNode, dt: Float): Unit {
        focusNode(node, false)
    }

    fun focusNode(node: CanvasNode, smooth: Boolean) {
        val pos = node.position
        val screen_pos = screenPosition(pos)
        if (smooth) {
            this.smoothScrollTo(screen_pos.x, screen_pos.y)
        } else {
            this.scrollTo(screen_pos.x, screen_pos.y)
        }
    }

    fun screenPosition(position: Point): Point {
        val new_position = Point(position)
        var curr_scene = scene
        if (curr_scene != null) {
            val pad = curr_scene.padding
            //new_position.y = -new_position.y
            new_position.x += pad.width
            new_position.y += pad.height
            new_position.x -= curr_scene.width / 2
            new_position.y -= curr_scene.height / 2
            new_position.x = new_position.x.coerceIn(0, curr_scene.scene!!.size.width - curr_scene.width)
            new_position.y = new_position.y.coerceIn(0,  curr_scene.scene!!.size.height - curr_scene.height)
        }
        return new_position
    }

    fun currentPosition(): Point {
        val pad = this.scene!!.padding
        return Point(
            scrollX + scene!!.width / 2 - pad.width,
            scrollY + scene!!.height / 2 - pad.height)
    }

}