

package com.lanyard.canvas
import android.graphics.*
import android.util.DisplayMetrics
import com.lanyard.helpers.plus
import com.lanyard.canvas.Size
import java.util.Collections.synchronizedList
import java.util.concurrent.CopyOnWriteArrayList


interface CanvasNodeTransform
{
    var position : Point
    var scale : SizeF
    var opacity : Float

    fun set(other: CanvasNodeTransform) {
        position = other.position
        scale = other.scale
        opacity = other.opacity
    }

    fun transform (other: CanvasNodeTransform) {
        position += other.position
        scale *= other.scale
        opacity *= other.opacity
    }

}

data class CanvasNodeTransformData(override var position : Point,
                          override var scale : SizeF,
                          override var opacity: Float) : CanvasNodeTransform
{
    constructor(other: CanvasNodeTransform) : this(other.position, other.scale, other.opacity)
    constructor() : this(Point(0,0), SizeF(1.0f,1.0f), 1.0f)
    init {

    }

    fun  transformed (other: CanvasNodeTransform) : CanvasNodeTransformData {
        return CanvasNodeTransformData(
            position + other.position,
            scale * other.scale,
            opacity * other.opacity)
    }
}

open class CanvasNode() : CanvasNodeTransform
{
    companion object {
        var debugBounds = false
    }

    protected val _children = synchronizedList<CanvasNode>(ArrayList())
    protected val _actions = synchronizedList<CanvasAction>(ArrayList())
    override var position = Point(0,0)
    open var magnitude = Size(0,0)
    override var opacity = 1.0f
    private var _sortChildren = false

    val size : Size
    get() {
        return magnitude * scale
    }

    var view : CanvasView? = null

    var parent : CanvasNode? = null
        set(value) {
        if ( field == value ) { return }
        if ( field != null ) {
            field!!._children.removeAll { it == this }
            this.view = null
        }
        if ( value != null) {
            value!!._children.add(this)
            this.view = value.view
        }
        field = value
    }

    open var anchor = PointF(0.5f,0.5f)
    override var scale = SizeF(1.0f,1.0f)

    var zOrder = 0
    var tag = ""
    var hidden : Boolean = false
        set (value){
        field = value
    }

    init {
        //_children = mutableListOf()
        //_actions = mutableListOf()
        hidden = false
    }

    constructor( x: Int, y: Int ) : this()  {
        position.set(x,y)
    }

    fun addChild( node: CanvasNode ) {
        _sortChildren = true
        node.parent = this
    }

    fun removeAllChildren( ) {
        _children.clear()
    }

    fun removeChild( node: CanvasNode ) {
        _sortChildren = true
        _children.remove(node)
    }

    fun removeAction ( tag: String ) {
        _actions.removeAll { it.tag == tag }
    }

    val children : List<CanvasNode>
        get() { return _children }



    open protected fun bounds(transform: CanvasNodeTransform) : Rect
    {
        var left =      (transform.position.x + position.x - magnitude.width * scale.width * transform.scale.width * anchor.x).toInt()
        var top =       (transform.position.y + position.y - magnitude.height * scale.height * transform.scale.height * (1-anchor.y)).toInt()
        var right =     (left + magnitude.width * scale.width * transform.scale.width).toInt()
        var bottom =    (top + magnitude.height * scale.height * transform.scale.height).toInt()
        return Rect(left,top,right,bottom)
    }

    fun update(dt: Long) {
        synchronized(_actions) {
            var itr = _actions.iterator()
            while (itr.hasNext()) {
                //for (a in _actions) {
                var a = itr.next()
                if (a.isValid()) {
                    if (a.running == false) {
                        a.start(this)
                    }
                    a.update(this, dt)
                } else {
                    itr.remove()
                }
            }
        }
        synchronized(_children) {
            if (_sortChildren) {
                _children.sortBy { chlid-> chlid.zOrder }
                _sortChildren = false
            }
            var children = _children.toTypedArray()
            var itr = children.iterator()
            while (itr.hasNext()) {
                var node = itr.next()
                if (node.parent == this) {
                    node.update(dt)
                }
            }
        }
    }

    open fun draw ( canvas: Canvas, transform: CanvasNodeTransformData, view: Rect, timestamp: Long ) {
        transform.transform(this)
        synchronized(_children) {
            var itr = _children.iterator()
            while (itr.hasNext()) {
                var node = itr.next()
                if (node.hidden == true) {
                    continue
                }
                node.draw(canvas, transform.copy(), view, timestamp)
                if (debugBounds) {
                    val paint = Paint()
                    var bounds = node.bounds(transform.copy())
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 4.0f
                    paint.color = Color.parseColor("#ffffffff")
                    canvas.drawRect(bounds, paint)
                }
            }
        }
    }

    fun run ( action: CanvasAction) {
        run(action,action.tag)
    }

    fun run ( action: CanvasAction, tag: String? ) {
        _actions.add(action)
        action.tag = tag
    }

}