package com.lanyard.canvas

import android.graphics.Point


data class Size (var width:Int, var height:Int){

    operator fun times(other: Size) : Size {
        return Size(width * other.width, height * other.height)
    }

    /*operator fun plus(other: Size) : Size {
        return Size(width + other.width, height + other.height)
    }*/

    operator fun times(other: Float) : Size {
        return Size((width * other).toInt(), (height * other).toInt())
    }


    operator fun times(other: SizeF): Size {
        return Size((width * other.width).toInt(), (height * other.height).toInt())
    }

    operator fun plus(size: Size): Size {
        return Size(width + size.width, height + size.height)

    }

    fun toPoint() :Point {
        return  Point(width, height)
    }
}