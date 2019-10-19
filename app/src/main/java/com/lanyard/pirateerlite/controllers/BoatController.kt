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

package com.lanyard.pirateerlite.controllers

import android.graphics.Point
import android.os.Handler
import com.lanyard.helpers.set
import com.lanyard.library.Graph
import com.lanyard.library.Vertex
import com.lanyard.pirateerlite.fragments.MapFragment
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.models.WorldNode
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.views.BoatView
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.*


class BoatController (model: BoatModel, view: BoatView) {

    companion object {
        private var _mapController = WeakReference<MapFragment>(null)
        fun setController (controller: MapFragment) {
            _mapController = WeakReference(controller)
        }
    }
    val model : BoatModel
    val view : BoatView
    private val _handler : Handler

    val isSailing : Boolean get() {
        return this.model.town == null
    }

    init {
        this._handler = Handler()
        this.model = model
        this.view = view
        if (model.town != null) {
            var source : Vertex<WorldNode>? = null
            Map.instance.graph.vertices.forEach { if (it.data === model.town) { source = it }}
            if (source != null) {
                view.sprite.position.set(source!!.position)
            }
        }
    }

    fun destroy() {
        _handler.removeCallbacksAndMessages(null)
    }

    fun sail() : Boolean {
        view.removePaths()
            for (job in model.cargo) {
                if (job != null) {
                    model.town?.removeJob(job)
                }
            }

        for (i in 1 until this.model.course.size) {
            val path = Map.instance.getRoute(this.model.course[i-1], this.model.course[i])
            view.addPath(Graph.getRoutePositions(path))
            var time = this.model.getSailingTime( view.length )
            if (model.departureTime != 0L &&
                model.departureTime + time < Date().time) {
                this.arrived(path.last().next.data as TownModel, true)
            } else {
                if (model.departureTime != 0L) {
                    time -= Date().time - model.departureTime
                }
                _handler.postDelayed({
                    this.arrived(path.last().next.data as TownModel)
                }, time)
            }
        }
        // if the the boats town is nil that means it has already departed
        if (model.course.size > 0) {
            if (this.model.town != null) {
                this.model.sail( view.length )
            } else {
                this.model.setCourseTime( view.length )
            }
            view.sail( Date(this.model.departureTime ), this.model.courseTime )
            return true
        }
        return false
    }

    private fun arrived (town: TownModel, quiet : Boolean = false) {
        this.model.arrive(town, quiet)
        if (this.model.isMoored) {
            view.sprite.hidden = true
            view.sprite.removeAction("sail")
            _mapController.get()?.boatArrived(this)
        }
    }
}