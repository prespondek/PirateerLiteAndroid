package com.lanyard.library

import android.graphics.Point
import android.graphics.PointF


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
        for (length in 0..lengths.size - 1) {
            path[length].getUniform((segments * lengths[length]).toInt())
            path[length].getUniform((segments * lengths[length]).toInt())
        }
    }

    fun splinePosition(time: Float): PointF {
        var offset: Float = 0.0F
        var index: Int = 0
        for (idx in 0..lengths.size - 1) {
            if (lengths[idx] + offset > time) {
                index = idx
                break
            }
            offset += lengths[idx]
        }
        val realtime = (time - offset) * (1 / lengths[index])
        return path[index].evaluateCurve(realtime)
    }
}