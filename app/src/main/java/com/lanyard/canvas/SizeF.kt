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