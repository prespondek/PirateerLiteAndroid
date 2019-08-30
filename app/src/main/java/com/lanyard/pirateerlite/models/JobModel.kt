package com.lanyard.pirateerlite.models

import com.lanyard.library.Graph
import com.lanyard.pirateerlite.controllers.JobController
import com.lanyard.pirateerlite.data.JobData
import com.lanyard.pirateerlite.singletons.Map

class JobModel {
    var source: TownModel

        get() { return Map.instance.towns[_data.sourceTownId] }
        set(town) {
            _data.destinationTownId = Map.instance.towns.indexOfFirst { it === town }
            calcValue()
        }

    var destination: TownModel

        get() { return Map.instance.towns[_data.destinationTownId] }
        set(town) {
            _data.destinationTownId = Map.instance.towns.indexOfFirst { it === town }
            calcValue()
        }


    private var _data : JobData

    val value: Int
        get() {
            return _data.value.toInt()
        }

    val isGold: Boolean
        get() {
            return _data.gold
        }

    val data: JobData get() = _data

    private fun calcValue ()
    {
        val route = Map.instance.getRoute(source, destination)
        _data.value = Graph.getRouteDistance(route) / BoatModel.scale
    }


    constructor(data:JobData)
    {
        _data = data
    }

    constructor(source: TownModel, destination: TownModel) {
        _data = JobData(
            JobController.jobData.random()[0],
            0.0f,
            Math.random() * 10.0 <= 1.0,
            Map.instance.towns.indexOfFirst { it === source },
            Map.instance.towns.indexOfFirst { it === destination },
            1.0
        )
        calcValue()
        if (_data.gold) {
            _data.value *= 0.01f
        }
    }

    val type get() = _data.type
}