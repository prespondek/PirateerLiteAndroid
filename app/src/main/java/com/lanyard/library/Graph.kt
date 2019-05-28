package com.lanyard.library

import kotlin.math.sqrt

data class Point( var x: Float, var y: Float) {
    override operator fun equals (other: Any?) : Boolean {
        if (other is Point) {
            return other.x == x && other.y == y
        }
        return false
    }
    operator fun minus (other: Any?) : Point {
        if (other is Point) {
            return Point(this.x - other.x,
            this.y - other.y)
        }
        return Point(0.0F,0.0F)
    }
    fun lenght() : Float {
        return sqrt( x*x + y*y );
    }

    fun distance(other: Point) : Float {
        return (this - other).lenght()
    }



}

class Vertex<T> ( data: T, position: Point ) {
    companion object Functor{
        fun <T> compareRange (lhs: Vertex<T>, rhs: Vertex<T>) : Boolean {
            return (lhs.score < rhs.score)
        }
    }

    var data :      T
    var position =  Point(0.0F, 0.0F)
    var score =     Float.MAX_VALUE
    var visited =   false
    var outEdges =  mutableSetOf<Edge<T>>()
    var inEdges =   mutableSetOf<Edge<T>>()

    init {
        this.data = data
        this.position = position
    }

    override operator fun equals (other: Any?) : Boolean {
        if (other is Vertex<*>) {
            return position == other.position
        }
        return false
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
        edge.weight = this.position.distance(other.position)
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

    override operator fun equals (other: Any?) : Boolean {
        if ( other is Edge<*> ) {
            return this.next.position == other.next.position &&
                    this.source.position == other.source.position
        }
        return false
}

}

class Graph<T> (vertices: ArrayList<Vertex<T>>)
{

    var vertices : ArrayList<Vertex<T>>

    enum class Algorithm {
        Djikstra
    }

    init {
        this.vertices = vertices
    }

    fun getRoute(algorithm: Algorithm, start: Vertex<T>, end: Vertex<T>) : ArrayList<Edge<T>>
    {
        for ( vertex in vertices ) {
            vertex.score = Float.MAX_VALUE
            vertex.visited = false
        }

        when (algorithm) {
            Algorithm.Djikstra -> {
                primeGraphDjikstra(start, end)
                return getRouteDjikstra(start, end)
            }

        }
    }

    fun getRouteDistance(route: Array<Edge<T>> ) : Float
    {
        var distance : Float = 0.0F
        route.forEach { distance += it.weight }
        return distance
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
            //print("Visiting " + String(top.id) + " with score of " + String(Int(top.score)));
            for ( edge in top.outEdges ) {
                if ( edge.next.visited == false ) {
                    if ( edge.next.score > (top.score + edge.weight )) {
                         edge.next.score = top.score + edge.weight
                        //print("Point " + String(edge.next.id) + " set score to: " + String(Int(edge.next.score)))
                    }
                    queue.enqueue( edge.next )
                    top.visited = true;
                }
            }
        }
    }

}