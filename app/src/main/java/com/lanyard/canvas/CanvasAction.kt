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

import android.graphics.Point
import com.lanyard.helpers.minus
import com.lanyard.helpers.plus
import com.lanyard.helpers.set
import com.lanyard.helpers.times
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor
import kotlin.math.min

abstract class CanvasAction {
    var _running = false
    var tag : String? = null
    val running : Boolean get() {
        return _running
    }
    open fun isValid() : Boolean {
        return true
    }
    abstract fun update( node: CanvasNode, dt: Long )
    open fun start( node: CanvasNode ) {
        _running = true
    }
}

abstract class CanvasActionInterval (duration: Long, interval: Long) : CanvasAction() {
    var duration: Long
    var interval: Long
    protected var _timer: Long
    private var _interval_timer: Long
    private var test = Date()

    override fun isValid() : Boolean {
        if (_timer < duration) {
            return true
        } else {
            return false
        }
    }
    open fun reset() {
        _timer = 0
        _interval_timer = 0
    }

    init {
        _timer = 0
        _interval_timer = 0
        this.duration = duration
        this.interval = interval
    }

    override fun start(node: CanvasNode) {
        super.start(node)
        test = Date()
    }

    override fun update( node: CanvasNode, dt: Long ) {
        _timer = min(_timer + dt, duration )
        if ( _timer - _interval_timer >= interval ) {
            step(node, _timer.toFloat() / duration)
            _interval_timer = _timer
        }
    }

    abstract open internal fun step ( node: CanvasNode, dt: Float )
}

class CanvasActionRepeat (action: CanvasActionInterval) : CanvasAction()
{
    private var _timer: Long
    var action : CanvasActionInterval
    init {
        _timer = 0
        this.action = action
    }

    override fun update( node: CanvasNode, dt: Long ) {
        _timer = _timer + dt
        if (_timer >= action.duration) {
            _timer = 0
            action.reset()
        }
        action.update(node, dt)
    }
}

class CanvasActionSequence (vararg actions: CanvasAction) : CanvasActionInterval(0,0)
{
    private var _actions : ArrayList<CanvasAction>
    private var _action_idx : Int = 0
    init {
        this._actions = ArrayList<CanvasAction>()
        for (action in actions) {
            this._actions.add(action)
            var intervalAction = action as? CanvasActionInterval
            if (intervalAction != null) {
                duration += intervalAction.duration
            }
        }
    }

    override fun reset() {
    super.reset()
        for (itr in _actions) {
            if (itr is CanvasActionInterval) {
                itr.reset()
            }
        }
}


    override fun isValid(): Boolean {
        return !_actions.isEmpty()
    }

    override fun update(node: CanvasNode, dt: Long) {
        _timer = min(_timer + dt, duration )
        var itr = _actions.iterator()
        while (itr.hasNext()) {
            var a = itr.next()
            if (a.isValid()) {
                if (a.running == false) {
                    a.start(node)
                }
                a.update(node, dt)
                if (a is CanvasActionInterval) {
                    break
                }
            }
        }
    }

    override fun step(node: CanvasNode, dt: Float) {

    }
}

class CanvasActionAnimate (frames: List<BitmapStream>, interval: Int): CanvasAction() {
    var bitmaps : List<BitmapStream>
    var interval : Int = 0
    private var _timer: Long
    var index : Int

    init {
        bitmaps = frames
        this.interval = interval
        _timer = 0
        index = 0
    }
    override fun update( node: CanvasNode, dt: Long ) {
        var sprite = node as? CanvasSprite
        assert(sprite != null)
        _timer += dt
        var step = 0
        while (_timer >= interval) {
            _timer -= interval
            step += 1
        }
        if (step > 0) {
            _timer = 0
            index += step
            if (index >= bitmaps.size) {
                var x = index.toFloat() / bitmaps.size
                x -= floor(x)
                index = (x * bitmaps.size).toInt()
            }
            changeFrame(sprite!!, index)
        }
    }
    fun changeFrame ( node: CanvasSprite, frame: Int ) {
        node.texture = bitmaps[frame]
    }
}

class CanvasActionCutom (duration: Long, interval: Long, action: (node: CanvasNode, dt: Float) -> Unit): CanvasActionInterval(duration,interval) {
    var action: (node: CanvasNode, dt: Float) -> Unit
    constructor(duration: Long, action: (node: CanvasNode, dt: Float) -> Unit) : this(duration, 0, action) {}
    init {
        this.action = action
    }
    override fun step(node: CanvasNode, dt: Float) {
        action(node,dt)
    }
}

class CanvasActionWait(duration: Long) : CanvasActionInterval(duration, 0)
{
    override fun step(node: CanvasNode, dt: Float) {

    }
}

abstract class CanvasActionInstant() : CanvasAction()
{
    private var done: Boolean = false
    override fun isValid(): Boolean {
        return !done
    }

    override fun update( node: CanvasNode, dt: Long ) {
        triggered(node)
        done = true
    }

    abstract fun triggered ( node: CanvasNode )
}

class CanvasActionRemoveFromParent() : CanvasActionInstant()
{
    override fun triggered(node: CanvasNode) {
        node.parent = null
    }
}

class CanvasActionInstantCustom(val action: (node: CanvasNode) -> Unit) : CanvasActionInstant() {
    override fun triggered(node: CanvasNode) {
        action(node)
    }
}

class CanvasActionScaleTo(duration: Long, scale: SizeF) : CanvasActionInterval(duration,0)
{
    protected val _scaleDelta: SizeF
    protected val _startScale: SizeF
    constructor(duration: Long, scale: Float) : this(duration, SizeF(scale,scale))
    init {
        _scaleDelta = scale
        _startScale = SizeF(0.0f,0.0f)
    }
    override fun start(node: CanvasNode) {
        super.start(node)
        _startScale.width = node.scale.width
        _startScale.height = node.scale.height
    }
    override fun step(node: CanvasNode, dt: Float) {
        node.scale.set(
            _startScale.width + _scaleDelta.width * dt,
            _startScale.height + _scaleDelta.height * dt)
    }
}

open class CanvasActionMoveBy(duration: Long, position: Point) : CanvasActionInterval(duration,0)
{
    protected val _positionDelta: Point
    protected val _startPosition: Point
    //protected var _previousPosition: Point

    init {
        _positionDelta = position
        _startPosition = Point(0,0)
        //_previousPosition = Point(0,0)
    }

    override fun start(node: CanvasNode) {
        super.start(node)
        //_previousPosition = Point(node.position)
        _startPosition.x = node.position.x
        _startPosition.y = node.position.y
    }

    override fun step(node: CanvasNode, dt: Float) {
        node.position.set(_startPosition + _positionDelta * dt)
    }
}

open class CanvasActionMoveTo(duration: Long, position: Point) : CanvasActionMoveBy(duration,position)
{
    override fun start(node: CanvasNode) {
        super.start(node)
        _positionDelta.set(_positionDelta - node.position)
    }
}

open class CanvasActionFadeTo(duration: Long, opacity: Float) : CanvasActionInterval(duration,0)
{
    protected var _endOpacity : Float
    protected var _startOpacity : Float

    init {
        _endOpacity = opacity
        _startOpacity = 0.0f
    }

    override fun start(node: CanvasNode) {
        super.start(node)
        _startOpacity = node.opacity
    }

    override fun step(node: CanvasNode, dt: Float) {
        node.opacity = _startOpacity + (_endOpacity - _startOpacity) * dt
    }
}

class CanvasActionFadeOut(duration: Long) : CanvasActionFadeTo(duration,0.0f)
{

}