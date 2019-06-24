package com.lanyard.canvas

import android.graphics.Point
import android.util.SizeF
import kotlin.math.floor
import kotlin.math.min

abstract open class CanvasAction {
    var _running = false
    var tag : String? = null
    val running : Boolean get() {
        return _running
    }
    open fun isValid() : Boolean {
        return true
    }
    fun start( ) {
        _running = true
    }
    abstract open fun update( node: CanvasNode, dt: Long )

}

abstract open class CanvasActionInterval (duration: Long, interval: Long) : CanvasAction() {
    var duration: Long
    var interval: Long
    private var _timer: Long
    private var _interval_timer: Long

    override fun isValid() : Boolean {
        return _timer <= duration
    }
    fun reset() {
        _timer = 0
        _interval_timer = 0
    }

    init {
        _timer = 0
        _interval_timer = 0
        this.duration = duration
        this.interval = interval
    }

    override fun update( node: CanvasNode, dt: Long ) {
        _timer = min(_timer + dt, duration )
        if (_timer - _interval_timer > interval) {
            step(node, _timer)
            _interval_timer = _timer
        }
    }

    abstract open protected fun step ( node: CanvasNode, dt: Long )
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
        _timer += dt
        if (_timer > action.duration) {
            _timer = ((_timer.toFloat() / action.duration - floor(_timer.toFloat())) * action.duration).toLong()
            action.reset()
        }
        action.update(node,_timer)
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

    override fun step(node: CanvasNode, dt: Long) {
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
    open fun changeFrame ( node: CanvasSprite, frame: Int ) {
        node.texture = bitmaps[frame]
    }
}

class CanvasActionCutom (duration: Long, interval: Long, action: (node: CanvasNode, dt: Long) -> Unit): CanvasActionInterval(duration,interval) {
    var action: (node: CanvasNode, dt: Long) -> Unit
    constructor(duration: Long, action: (node: CanvasNode, dt: Long) -> Unit) : this(duration, 0, action) {

    }
    init {
        this.action = action
    }
    override fun step(node: CanvasNode, dt: Long) {
        action(node,dt)
    }
}

class CanvasActionWait(duration: Long) : CanvasActionInterval(duration, 0)
{
    override fun step(node: CanvasNode, dt: Long) {

    }
}

class CanvasActionRemoveFromParent() : CanvasAction()
{
    override fun update( node: CanvasNode, dt: Long ) {
        node.parent = null
    }
}

class CanvasActionScaleTo(duration: Long, scale: Float) : CanvasActionInterval(duration,0)
{
    var scale : Float
    init {
        this.scale = scale
    }
    override fun step(node: CanvasNode, dt: Long) {
        var delta = dt.toFloat() / duration
        node.scale = SizeF((scale - node.scale.width) * delta, (scale - node.scale.height) * delta)
    }
}

class CanvasActionMoveTo(duration: Long, position: Point) : CanvasActionInterval(duration,0)
{
    override fun step(node: CanvasNode, dt: Long) {

    }
}

class CanvasActionMoveBy(duration: Long, offset: Point) : CanvasActionInterval(duration,0)
{
    override fun step(node: CanvasNode, dt: Long) {

    }
}