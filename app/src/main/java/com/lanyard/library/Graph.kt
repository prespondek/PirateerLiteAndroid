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
import com.lanyard.helpers.distance

/**
 * Your basic two dimensional spacial graph with any number of vertices and edges. Can find the shortest distance
 * between two points on the graph using Dijkstras algorithm.
 *
 * @author Peter Respondek
 */

class Vertex<T> ( data: T, position: Point) {
    companion object Functor{
        fun <T> compareRange (lhs: Vertex<T>, rhs: Vertex<T>) : Boolean {
            return (lhs.score < rhs.score)
        }
    }

    var data :      T
    var position =  Point(0, 0)
    var score =     Float.MAX_VALUE
    var visited =   false
    var outEdges =  mutableSetOf<Edge<T>>()
    var inEdges =   mutableSetOf<Edge<T>>()

    init {
        this.data = data
        this.position = Point(position)
    }

    override operator fun equals (other: Any?) : Boolean {
        if (other is Vertex<*>) {
            return position == other.position
        }
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }


    fun getEdge (other: Vertex<T>) : Edge<T>? {
    for ( edge in this.outEdges ) {
        if ( edge.next === other ) {
            return edge
        }
    }
    return null
}



    fun linkVertex(other: Vertex<T>)
    {
        val edge = Edge(this,  other)
        edge.weight = this.position.distance(other.position).toFloat()
        this.outEdges.add(edge)
        other.inEdges.add(edge)
    }
}

class Edge<T> (source: Vertex<T>, target: Vertex<T>) {
    val next :      Vertex<T>
    val source :    Vertex<T>
    var weight :    Float = 0.0F
    var range :     Float = 0.0F

    init{
        this.next = target
        this.source = source
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override operator fun equals (other: Any?) : Boolean {
        if ( other is Edge<*> ) {
            return this.next.position == other.next.position &&
                    this.source.position == other.source.position
        }
        return false
}

}

class Graph<T>
{

    companion object {
        fun <T> getRouteDistance(route: List<Edge<T>> ) : Float
        {
            var distance : Float = 0.0F
            route.forEach { distance += it.weight }
            return distance
        }
        fun <T> getRoutePositions ( path: List<Edge<T>> ) : List<Point> {
            var points = ArrayList<Point>()
            points.add( path[0].source.position )
            points.addAll(path.map { segment->
                segment.next.position
            })
            return points
        }
    }

    var vertices : ArrayList<Vertex<T>>

    enum class Algorithm {
        Dijkstra
    }
    constructor() {
        this.vertices = ArrayList<Vertex<T>>()
    }

    constructor(vertices: ArrayList<Vertex<T>>) {
        this.vertices = vertices
    }

    fun getRoute(algorithm: Algorithm, start: Vertex<T>, end: Vertex<T>) : ArrayList<Edge<T>>
    {
        for ( vertex in vertices ) {
            vertex.score = Float.MAX_VALUE
            vertex.visited = false
        }

        when (algorithm) {
            Algorithm.Dijkstra -> {
                primeGraphDjikstra(start, end)
                return getRouteDjikstra(start, end)
            }

        }
    }

    private fun getRouteDjikstra( start: Vertex<T>, end: Vertex<T> ) : ArrayList<Edge<T>>
    {
        var route = ArrayList<Edge<T>>()
        var node = end;

        while (node !== start) {
            var link : Edge<T>? = null
            for ( edge in node.inEdges ) {
                if (node.score > edge.source.score) {
                    node = edge.source
                    link = edge
                }
            }
            route.add(link!!)
        }
        route.reverse()
        return route
    }

    private fun primeGraphDjikstra ( start: Vertex<T>, end: Vertex<T> )
    {
        var queue = PriorityQueue<Vertex<T>>( Vertex.Functor::compareRange )
        start.score = 0.0F
        queue.enqueue( start )
        while ( !queue.isEmpty() ) {
            val top = queue.peek()!!
            if ( top === end ) { break }
            queue.dequeue()
            if ( top.visited == true ) { continue }
            for ( edge in top.outEdges ) {
                if ( edge.next.visited == false ) {
                    if ( edge.next.score > (top.score + edge.weight )) {
                         edge.next.score = top.score + edge.weight
                    }
                    queue.enqueue( edge.next )
                    top.visited = true;
                }
            }
        }
    }

}