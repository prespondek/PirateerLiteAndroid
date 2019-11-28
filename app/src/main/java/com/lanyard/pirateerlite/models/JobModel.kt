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

package com.lanyard.pirateerlite.models

import com.lanyard.library.Graph
import com.lanyard.pirateerlite.data.JobData
import com.lanyard.pirateerlite.singletons.Map

class JobModel {
    companion object {
        var jobData = ArrayList<ArrayList<String>>()
    }

    var source: TownModel

        get() { return Map.instance.towns[_data.sourceTownId.toInt()] }
        set(town) {
            _data.destinationTownId = Map.instance.towns.indexOfFirst { it === town }.toLong()
            calcValue()
        }

    var destination: TownModel

        get() { return Map.instance.towns[_data.destinationTownId.toInt()] }
        set(town) {
            _data.destinationTownId = Map.instance.towns.indexOfFirst { it === town }.toLong()
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
            jobData.random()[0],
            0.0f,
            Math.random() * 10.0 <= 1.0,
            Map.instance.towns.indexOfFirst { it === source }.toLong(),
            Map.instance.towns.indexOfFirst { it === destination }.toLong(),
            1.0
        )
        calcValue()
        if (_data.gold) {
            _data.value *= 0.01f
        }
    }

    val type get() = _data.type
}