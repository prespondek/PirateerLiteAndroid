package com.lanyard.pirateerlite.models

import com.lanyard.library.Graph
import com.lanyard.pirateerlite.singletons.Map

class JobModel (source: TownModel, destination: TownModel) {
    var source: TownModel

        get() { return Map.sharedInstance.towns[_sourceIndex] }
        set(town) {
            _destinationIndex = Map.sharedInstance.towns.indexOfFirst { it === town }
            calcValue()
        }

    var destination: TownModel

        get() { return Map.sharedInstance.towns[_destinationIndex] }
        set(town) {
            _destinationIndex = Map.sharedInstance.towns.indexOfFirst { it === town }
            calcValue()
        }


    var type: String
    private var _value: Float
    private var _gold = false
    private var _sourceIndex: Int
    private var _destinationIndex: Int
    var multiplier: Double

    val value: Int
        get() {
            return _value.toInt()
        }

    val isGold: Boolean
        get() {
            return _gold
        }


    private fun calcValue ()
    {
        val route = Map.sharedInstance.getRoute(source, destination)
        _value = Graph.getRouteDistance(route)
    }

    init
    {
        _destinationIndex = Map.sharedInstance.towns.indexOfFirst { it === destination }
        _sourceIndex = Map.sharedInstance.towns.indexOfFirst { it === source }
        this.multiplier = 1.0
        //this.type = JobController.jobData.randomElement()[0]
        this.type = ""
        if (Math.random() * 9 + 1 == 1.0) {
        _gold = true
    }
        this._value = 0.0f
        calcValue()
        if (_gold) {
            _value *= 0.01f
        }
    }
}