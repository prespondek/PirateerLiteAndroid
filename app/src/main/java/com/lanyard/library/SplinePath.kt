package com.lanyard.library

class SplinePath {
    private var path: ArrayList<CardinalSpline>
    var lengths: ArrayList<Float>
        private set

    var length: Float = 0.0F
        private set

    fun count(): Int {
        return path.size
    }

    init {
        path = ArrayList<CardinalSpline>()
        lengths = ArrayList<Float>()
    }

    fun pathLength(index: Int): Float {
        return path[index].length
    }

    fun addPath(path: ArrayList<Point>) {
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
            path[length].getUniform((segments * lengths[length]) as Int)
            path[length].getUniform((segments * lengths[length]) as Int)
        }
    }

    fun splinePosition(time: Float): Point {
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