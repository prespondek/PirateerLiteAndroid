package com.lanyard.canvas

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Size
import android.util.SizeF
import com.lanyard.helpers.plus


open class CanvasNode()
{
    companion object {
        val density : Float
        get() {
            return DisplayMetrics.DENSITY_DEVICE_STABLE.toFloat() / DisplayMetrics.DENSITY_DEFAULT.toFloat()
        }
    }
    protected val _children : MutableList<CanvasNode>
    protected val _actions : MutableList<CanvasAction>
    var position = Point(0,0)
    open var size = Size(0,0)
    var parent : CanvasNode? = null
    set(value) {
        if ( field == value ) { return }
        if ( field != null ) {
            field!!._children.removeAll { it == this }
        }
        if ( value != null) {
            value!!._children.add(this)
        }
        field = value
    }

    var anchor = PointF(0.5f,0.5f)
    var scale = SizeF(1.0f,1.0f)
    var zOrder = 0
    var tag = ""
    var hidden : Boolean = false
        set (value){
        field = value
    }


    init {
        _children = mutableListOf()
        _actions = mutableListOf()
        hidden = false
    }

    constructor( x: Int, y: Int ) : this()  {
        position.set(x,y)
    }

    fun addChild( node: CanvasNode ) {
        node.parent = this
    }

    fun removeAllChildren( ) {
        _children.clear()
    }

    fun removeAction ( tag: String ) {
        _actions.removeAll { it.tag == tag }
    }

    val children : List<CanvasNode>
        get() { return _children }



    open protected fun bounds(pos: Point) : Rect
    {
        var left = (pos.x + position.x - size.width * scale.width * anchor.x).toInt()
        var top = (pos.y + position.y - size.height * scale.height * (1-anchor.y)).toInt()
        var right = (left + size.width * scale.width).toInt()
        var bottom = (top + size.height * scale.height).toInt()
        return Rect(left,top,right,bottom)
    }

    fun update(dt: Long) {
        for ( node in _children ) {
            node.update(dt)
        }
        for (a in _actions) {
            if (a.isValid()) {
                a.update(this, dt)
            }
        }
    }

    open fun draw(canvas: Canvas, pos: Point) {
        for ( node in _children ) {
            if (node.hidden == true) { continue }
            node.draw(canvas, position + pos)
        }
    }

    fun run ( action: CanvasAction, tag: String? = null ) {
        _actions.add(action)
        action.tag = tag
    }

}