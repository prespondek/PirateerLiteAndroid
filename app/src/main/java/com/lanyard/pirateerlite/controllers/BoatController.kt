package com.lanyard.pirateerlite.controllers

import android.graphics.Point
import android.os.Handler
import com.lanyard.library.Graph
import com.lanyard.library.Vertex
import com.lanyard.pirateerlite.fragments.MapFragment
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.models.WorldNode
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.views.BoatView
import java.util.*


class BoatController (model: BoatModel, view: BoatView) {

    companion object {
        private var _mapController : MapFragment? = null
        fun setController (controller: MapFragment) {
            BoatController._mapController = controller
        }
    }
    var model : BoatModel
    var view : BoatView

    val isSailing : Boolean get() {
        return this.model.town == null
    }

    init {
        this.model = model
        this.view = view
        if (model.town != null) {
            var source : Vertex<WorldNode>? = null
            Map.sharedInstance.graph.vertices.forEach { if (it.data === model.town) { source = it }}
            if (source != null) {
                view.sprite.position = Point(source!!.position)
            }
        }
    }


    fun sail() {

            for (job in model.cargo) {
                if (job != null) {
                    model.town?.removeJob(job)
                }
            }

        for (i in 1..this.model.course.size - 1) {
            val path = Map.sharedInstance.getRoute(this.model.course[i-1], this.model.course[i])
            view.addPath(Graph.getRoutePositions(path))
            var time = this.model.getSailingTime( view.length )
            if (model.departureTime != 0L &&
                model.departureTime + time < Date().time) {
                this.arrived(path.last().next.data as TownModel, true)
            } else {
                if (model.departureTime != 0L) {
                    time -= Date().time - model.departureTime
                }
                Handler().postDelayed({
                    this.arrived(path.last()!!.next.data as TownModel)
                }, time)
            }
        }
        // if the the boats town is nil that means it has already departed
        if (model.course.size > 0) {
            if (this.model.town != null) {
                this.model.sail( view.length )
            } else {
                this.model.setDistance( view.length )
            }
            view.sail( Date(this.model.departureTime ), this.model.courseTime )
        }
    }

    private fun arrived (town: TownModel, quiet : Boolean = false) {
        this.model.arrive(town, quiet)
        if (this.model.isMoored) {
            //BoatController._mapController?.boatArrived(this)
            //view.removePaths()
        }
    }
}