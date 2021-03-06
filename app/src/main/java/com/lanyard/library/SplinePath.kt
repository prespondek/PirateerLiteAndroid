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

package com.lanyard.library

import android.graphics.Point
import android.graphics.PointF
import androidx.core.math.MathUtils.clamp

/**
 * A list of curves representing a continuous path. Spline can be smoothed or position can be evaluated
 * across the entire path.
 *
 * @author Peter Respondek
 */


open class SplinePath {
    private var path: MutableList<CardinalSpline>
    var lengths: MutableList<Float>
        private set

    var length: Float = 0.0F
        private set

    fun count(): Int {
        return path.size
    }

    init {
        path = mutableListOf<CardinalSpline>()
        lengths = mutableListOf<Float>()
    }

    fun pathLength(index: Int): Float {
        return path[index].length
    }

    fun addPath(path: List<Point>) {
        val new_path = CardinalSpline(path)
        this.path.add(new_path)
        val dist = new_path.length
        length += dist
        lengths.clear()
        this.path.forEach { lengths.add(it.length / length) }
    }

    fun removePaths() {
        path.clear()
        lengths.clear()
        length = 0.0F
    }

    fun smooth(segments: Int) {
        for (length in 0 until lengths.size) {
            path[length].getUniform((segments * lengths[length]).toInt())
            path[length].getUniform((segments * lengths[length]).toInt())
        }
    }

    fun splinePosition(time: Float): PointF {
        clamp(time,0.0f,1.0f)
        var offset: Float = 0.0F
        var index: Int = 0
        for (idx in 0 until lengths.size) {
            if (lengths[idx] + offset >= time) {
                index = idx
                break
            }
            offset += lengths[idx]
        }

        val realtime = (time - offset) * (1.0f / lengths[index])
        return path[index].evaluateCurve(realtime)

    }
}