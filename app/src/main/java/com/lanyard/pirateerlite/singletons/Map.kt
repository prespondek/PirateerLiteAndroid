package com.lanyard.pirateerlite.singletons

import android.graphics.Point
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.lanyard.library.Edge
import com.lanyard.library.Graph
import com.lanyard.library.Vertex
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.controllers.JobController
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.models.WorldNode
import java.io.InputStreamReader


class Map {
    val graph = Graph<WorldNode>()
    var towns: ArrayList<TownModel>

    companion object {
        private var _map: Map? = null
        fun init(scale: Float) {
            _map = Map()
            _map!!.setup(scale)
        }
        val sharedInstance: Map
            get() {
                if (_map != null) {
                    return _map!!
                } else {
throw ExceptionInInitializerError()
                    /*do {

                        _map = try JSONDecoder().decode(Map.self, from: Data(contentsOf: Map. JSONPath))
                        } catch {
                            _map = Map()
                        }
                        return _map!
                    }*/

                }
                //_map = Map()
                //return _map!!
                /*val JSONPath : URL
        get(){
            var path = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0]
            path += "/map.json"
            return URL(fileURLWithPath: path)
        }*/
            }
    }

    fun townPosition(town: TownModel): Point {
        var pos: Point? = null
        for (vert in graph.vertices) {
            if (town === vert.data) {
                pos = vert.position
                break
            }
        }
        assert(pos != null)
        return Point(pos)!!
    }

    fun getRoute(start: TownModel, end: TownModel): List<Edge<WorldNode>> {
        var source: Vertex<WorldNode>? = null
        var destination: Vertex<WorldNode>? = null
        graph.vertices.forEach { vert ->
            if (vert.data === start as WorldNode) {
                source = vert
            }
        }
        graph.vertices.forEach { vert ->
            if (vert.data === end as WorldNode) {
                destination = vert
            }
        }
        val path = graph.getRoute(Graph.Algorithm.Djikstra, source!!, destination!!)
        return path
    }

    fun mergeRoutes(
        source: Vertex<WorldNode>,
        paths: MutableList<List<Edge<WorldNode>>>
    ): MutableList<List<Edge<WorldNode>>> {

        fun splitPath(
            vert: Vertex<WorldNode>,
            path: MutableSet<Edge<WorldNode>>,
            extra: Edge<WorldNode>? = null
        ): MutableList<List<Edge<WorldNode>>> {
            var start = vert
            var parts = mutableListOf<List<Edge<WorldNode>>>()
            var stem = mutableListOf<Edge<WorldNode>>()
            if (extra != null) {
                stem.add(extra!!)
            }
            while (!path.isEmpty()) {
                val joining = start.outEdges.intersect(path)
                if (joining.size == 0) {
                    break
                }
                if (joining.size == 1) {
                    path.remove(joining.first())
                    stem.add(joining.first())
                    start = joining.first().next
                    continue
                } else {
                    for (join in joining) {
                        path.remove(join)
                        parts.addAll(splitPath(join.next, path, join))
                    }
                }
            }
            if (stem.size > 0) {
                parts.add(stem)
            }
            return parts
        }
        if (paths.size == 1) {
            return paths
        }

        var routes = mutableSetOf<Edge<WorldNode>>()
        for (path in paths) {
            for (vert in path) {
                routes.add(vert)
            }
        }
        return splitPath(source, routes)
    }

    init {
        this.towns = ArrayList<TownModel>()
    }


    fun setup(scale : Float) {
        val gson = Gson()
        val reader = JsonReader(InputStreamReader (MapActivity.instance.assets.open("map_model.json")))
        val config = gson.fromJson<HashMap<String, Any>>(reader, object : TypeToken<HashMap<String, Any>>() {}.type)

        val vertexConfig =  config["Vertices"]      as ArrayList<ArrayList<Int>>
        val edgeConfig =    config["Edges"]         as ArrayList<ArrayList<Double>>
        val townInfo =      config["TownInfo"]      as ArrayList<ArrayList<Any>>
        val townIndices =   config["TownIndex"]     as ArrayList<Int>
        val townCost =      config["TownCost"]      as ArrayList<Int>
        val townUpgrade =   config["TownUpgrade"]   as ArrayList<ArrayList<Int>>
        val jobData =       config["Jobs"]          as ArrayList<ArrayList<String>>


        for (i in 0..vertexConfig.size - 1) {
            var vert_data = vertexConfig[i]
            graph.vertices.add(Vertex(WorldNode(), Point((vert_data[0] * scale).toInt(), (-vert_data[1] * scale).toInt())))
        }
        TownModel.setGlobals(townCost, townUpgrade)
        if (towns.size == 0) {
            for (i in 0..townInfo.size - 1) {
                val town = TownModel(townInfo[i])
                this.towns.add(town)
            }
        }
        for (i in 0..towns.size - 1) {
            towns[i].setup(townInfo[i])
            graph.vertices[townIndices[i]].data = towns[i]
        }

        for (i in 0..edgeConfig.size - 1) {
            for (j in edgeConfig[i]) {
                graph.vertices[i].linkVertex(graph.vertices[j.toInt()])
            }
        }
        JobController.jobData = jobData

    }
}