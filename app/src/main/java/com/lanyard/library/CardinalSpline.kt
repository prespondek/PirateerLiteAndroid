package com.lanyard.library
import android.graphics.Point
import android.graphics.PointF
import com.lanyard.helpers.distance
import com.lanyard.helpers.toPoint
import kotlin.math.min


class CardinalSpline (path : List<Point>, tension: Float = 0.5F ) {
    data class DistanceTableEntry ( var t: Float, var distance: Float ) {
    }

    var length : Float = 0.0F
    var path : MutableList<Point>

    var tension : Float

    init {
        this.path = path.toMutableList()
        this.tension = tension
        var dist : Float = 0.0F
        if ( this.path.size != 0 ) {
            for ( segment in 1 until this.path.size ) {
                dist += this.path[segment - 1].distance( this.path[segment] ).toFloat()
            }
        }
        this.length = dist
    }

    fun lerp(a: Float,b: Float,f: Float) : Float
    {
        return a + f * (b - a)
    }

    private fun evaluate(p0: Point, p1: Point, p2: Point, p3: Point, tension: Float, t: Float) : PointF
    {
        val t2 = t * t
        val t3 = t2 * t

        val s = (1 - tension) / 2

        val b1 = s * ((-t3 + (2 * t2)) - t)                      // s(-t3 + 2 t2 - t)P1
        val b2 = s * (-t3 + t2) + (2 * t3 - 3 * t2 + 1)          // s(-t3 + t2)P2 + (2 t3 - 3 t2 + 1)P2
        val b3 = s * (t3 - 2 * t2 + t) + (-2 * t3 + 3 * t2)      // s(t3 - 2 t2 + t)P3 + (-2 t3 + 3 t2)P3
        val b4 = s * (t3 - t2);                                   // s(t3 - t2)P4

        val x = (p0.x*b1 + p1.x*b2 + p2.x*b3 + p3.x*b4)
        val y = (p0.y*b1 + p1.y*b2 + p2.y*b3 + p3.y*b4)

        return PointF(x,y)
    }


    // Evaluates a curve with any number of points using the Catmull-Rom method
    fun evaluateCurve( time: Float ) : PointF
    {
        var p : Int
        var lt : Float
        val deltaT = 1.0F / (path.size - 1)

        if ( time == 1.0F ) {
            p = path.size - 1
            lt = 1.0F
        } else {
            p = (time / deltaT).toInt()
            lt = (time - deltaT * p) / deltaT;
        }
        var i0 : Int = p-1
        if (p == 0) { i0 = 0 }
        val i1 : Int = p
        val i2 : Int = min(path.size - 1, p+1)
        val i3 : Int = min(path.size - 1, p+2)

        // Interpolate
        val pp0 = path[i0]
        val pp1 = path[i1]
        val pp2 = path[i2]
        val pp3 = path[i3]

        return evaluate(pp0, pp1, pp2, pp3, tension, lt)

    }

    private fun createDistanceTable( table: MutableList<DistanceTableEntry> )
    {
        val numPointsMin1 = path.size - 1;

        var distSoFar = 0.0F

        val start = DistanceTableEntry(0.0F,0.0F)
        table.add(start)
        for ( i in 1 until path.size ){
            val dist = path[i-1].distance(path[i])
            distSoFar += dist.toFloat()
            val curr = DistanceTableEntry( i.toFloat() / numPointsMin1, distSoFar )
            table.add( curr )
        }
    }

    fun getNonUniform( segments: Int )
    {
        var array = mutableListOf<Point>()
        val numPointsDesiredMin1 = segments-1
        for ( i in 0 until segments ) {
            val t = i as Float / numPointsDesiredMin1
            array.add(evaluateCurve(t).toPoint())
        }
        path = array
    }

    fun getUniform( segments: Int )
    {
        var array = mutableListOf<Point>()
        var distTable = mutableListOf<DistanceTableEntry>()
        createDistanceTable( distTable )
        val numPointsDesiredMin1 = segments-1
        val totalLength = distTable[path.size-1].distance;

        for ( i in 0 until segments ) {
            val distT = i.toFloat() / numPointsDesiredMin1
            val distance = distT * totalLength

            val t = timeValueFromDist( distance, distTable )
            var p = evaluateCurve( t ).toPoint()
            array.add(p)
        }
        path = array
    }

    private fun timeValueFromDist( dist: Float, table: MutableList<DistanceTableEntry> ) : Float
    {
        for ( i in path.size - 2 downTo 0 step 1 ) {
        val entry = table[i];
        if ( dist > entry.distance ) {
            if(i == this.path.size-1) {
                return 1.0F
            } else {
                val nextEntry = table[i+1];
                val lerpT = (dist - entry.distance) / (nextEntry.distance - entry.distance)
                val t = lerp(entry.t, nextEntry.t, lerpT);

                return t;
            }
        }
    }
        return 0.0F
    }

}