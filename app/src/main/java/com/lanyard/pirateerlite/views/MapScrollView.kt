package com.lanyard.pirateerlite.views

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.ViewTreeObserver.OnScrollChangedListener
import com.lanyard.library.SuperScrollView
import kotlinx.android.synthetic.main.fragment_map.view.*


class MapScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : SuperScrollView(context, attrs, defStyleAttr),
    OnScrollChangedListener
{
    var scrollListener : MapScrollViewListener? = null
    var pos = Point(0,0)

    init {

    }

    override fun onScrollChanged() {
        var newPos = Point(vscrollview.scrollX, vscrollview.scrollY)
        if (newPos != this.pos) {
            //HorizontalScrollView
            pos = newPos
            scrollListener?.onScrollChanged(pos)
        }
    }

/*
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var lhs = super.onTouchEvent(event)
        var rhs = this.hscrollview.onTouchEvent(event)
        if (event != null) {
            println("Touch " + event.action + " " + lhs.toString() + " " + rhs.toString())
        }
        return  rhs


    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        var lhs = super.onInterceptTouchEvent(event)
        var rhs = this.hscrollview.onInterceptTouchEvent(event)
        if (event != null) {
            println(event.action)
            var newPos = Point(hscrollview.scrollX, vscrollview.scrollY)
            println(event.action.toString() + " " + newPos.toString() + " " + this.pos.toString() + " " + lhs.toString() + " " + rhs.toString())
            if ( event.action == ACTION_MOVE || newPos != pos ) {
                return true
                println("boo")
            }
        }
        return false
    }*/
}


// It's 2019 and Google STILL hasn't made a multi-directional scrollview :(
interface MapScrollViewListener {
    fun onScrollChanged(offset: Point)
}