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

package com.lanyard.helpers

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lanyard.canvas.Size
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import android.view.animation.Animation
import androidx.fragment.app.Fragment


operator fun Point.plus(other: Point): Point {
    return Point(x + other.x, y + other.y)
}

operator fun Point.plus(other: Size): Point {
    return Point(x + other.width, y + other.height)
}

operator fun Point.minus(other: Point): Point {
    return Point(x - other.x, y - other.y)
}

class ColorHelper {
    companion object {
        fun ARGBInt(alpha: Float, red: Float, green: Float, blue: Float): Int {
            return (alpha * 255.0f + 0.5f).toInt() shl 24 or
                    ((red * 255.0f + 0.5f).toInt() shl 16) or
                    ((green * 255.0f + 0.5f).toInt() shl 8) or
                    (blue * 255.0f + 0.5f).toInt()
        }
    }
}
fun Boolean.toInt() : Int {
    if (and(true)) return 1
    return 0
}



operator fun Point.minus(other: Size): Point {
    return Point(x - other.width, y - other.height)
}

operator fun Point.times(other: Float): Point {
    return Point((x * other).toInt(), (y * other).toInt())
}

operator fun Point.unaryMinus() : Point {
    return Point(-x,-y)
}
operator fun Point.timesAssign (other: Point) {
    x *= other.x
    y *= other.y
}

operator fun Point.timesAssign (other: Float) {
    x *= other.toInt()
    y *= other.toInt()
}

operator fun Point.minusAssign(other: Point) {
    x -= other.x
    y -= other.y
}

operator fun Point.minusAssign(other: Size) {
    x -= other.width
    y -= other.height
}

operator fun Point.minusAssign(other: Float) {
    x -= other.toInt()
    y -= other.toInt()
}

operator fun PointF.minus (other: PointF) : PointF {
    return PointF (this.x - other.x, this.y - other.y)
}
operator fun Point.plusAssign(other: Point) {
    x += other.x
    y += other.y
}

fun Point.set(other: PointF) {
    x = other.x.toInt()
    y = other.y.toInt()
}

fun <T> MutableList<T>.popLast() : T
{
    var last = last()
    removeAt(size-1)
    return last
}

val Point.angle : Double
    get() {
        return atan2(y.toDouble(), x.toDouble());
    }

val PointF.angle : Double
    get() {
        return atan2(y.toDouble(), x.toDouble());
    }

fun Point.lenght () : Double {
    return sqrt( (x*x + y*y).toDouble() );
}

fun Point.toPointF() : PointF {
    return PointF(x.toFloat(),y.toFloat())
}

fun PointF.toPoint() : Point {
    return Point(x.toInt(),y.toInt())
}

fun Point.distance(other: Point) : Double {
    return (this - other).lenght()
}

fun Rect.intersects(rect: Rect) : Boolean {
    return intersects(min(rect.left,rect.right),min(rect.top,rect.bottom),max(rect.right,rect.left) ,max(rect.bottom,rect.top))
}

fun Point.set(other: Point) {
    set(other.x,other.y)
}

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)



