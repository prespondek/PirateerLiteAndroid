package com.lanyard.canvas

data class SizeF (var width:Float, var height:Float){
    constructor(other: SizeF) : this (other.width, other.height)
    constructor(other: Float) : this (other, other)
    operator fun SizeF.times(other: SizeF) : SizeF {
        return SizeF(width * other.width, height * other.height)
    }
    operator fun SizeF.times(other: Float) : SizeF {
        return SizeF(width * other, height * other)
    }
    operator fun plus(size: SizeF): SizeF {
        return SizeF(width + size.width, height + size.height)
    }
    /*operator fun timesAssign(size: SizeF) {
        width *= size.width
        height *= size.height
    }*/

    operator fun times(size: SizeF) : SizeF {
        return SizeF(width * size.width, height * size.height)
    }
}