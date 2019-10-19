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


data class Size (var width:Int, var height:Int){

    operator fun times(other: Size) : Size {
        return Size(width * other.width, height * other.height)
    }

    /*operator fun plus(other: Size) : Size {
        return Size(width + other.width, height + other.height)
    }*/

    fun set(other:Size) {
        width = other.width
        height = other.height
    }
    fun set(width: Int, height: Int) {
        this.width = width
        this.height = height
    }
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