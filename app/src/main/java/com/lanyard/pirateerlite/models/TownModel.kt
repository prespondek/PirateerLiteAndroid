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

import android.graphics.Color
import com.lanyard.pirateerlite.data.StorageJobData
import com.lanyard.pirateerlite.data.TownData
import com.lanyard.pirateerlite.data.TownJobData
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User
import kotlinx.coroutines.runBlocking

open class WorldNode

class TownModel (data: TownData): WorldNode(), User.UserListener {

    interface TownDelegate {
        fun jobsUpdated ()
    }

    companion object {
        private lateinit var townCost : ArrayList<Int>
        private lateinit var townUpgrade : ArrayList<ArrayList<Int>>
        fun setGlobals (townCost: ArrayList<Int>, townUpgrade: ArrayList<ArrayList<Int>>) {
            TownModel.townCost = townCost
            TownModel.townUpgrade = townUpgrade
        }
        val maxLevel : Int
            get() { return TownModel.townUpgrade.size - 1 }
    }

    enum class TownType {
        island, castle, pub, village, lighthouse, prison, fishermen, mansion, homestead;
    }


    enum class HarbourSize {
        marina, docks, pier
    }

    enum class Allegiance {
        none, british, american, spanish, french;

        companion object {
            val values = arrayOf(none,british, american, spanish, french)
        }
    }

    var color = Color.BLACK
    var delegate : TownDelegate? = null
    private var _boats = ArrayList<BoatModel>()
    private var _jobs : ArrayList<JobModel>
    private var _storage : ArrayList<JobModel?>
    private var _data : TownData

    var level : Int
        get() = _data.level
        set (value){
            if (value > TownModel.maxLevel) {
                _data.level = TownModel.maxLevel
            } else {
                _data.level = value
            }
            while (_storage.size < TownModel.townUpgrade[_data.level][1]) {
                _storage.add(null)
            }
        }

    init {
        _jobs =         ArrayList()
        _storage = ArrayList()
        _data = data
        level = _data.level
    }

    val id              get() = _data.id
    val allegiance      get() = _data.allegiance
    val harbour         get() = _data.harbour
    val description     get() = _data.description
    val name            get() = _data.name
    val type            get() = _data.type
    val totalVisits     get() = _data.totalVisits
    val endSilver       get() = _data.endSilver
    val endGold         get() = _data.endGold
    val startGold       get() = _data.startGold
    val startSilver     get() = _data.startSilver
    val jobsTimeStamp   get() = _data.jobsTimeStamp




    val storage : ArrayList<JobModel?>
        get() { return _storage }

    val jobs : List<JobModel>?
        get () {
            if (level == 0) {
                return null
            }
            if (_data.jobsTimeStamp != User.instance.jobDate) {
                refreshJobs()
            }
            return _jobs
        }

    val jobsDirty : Boolean
        get() { return _data.jobsTimeStamp != User.instance.jobDate }

    val jobsSize : Int get () { return TownModel.townUpgrade[level][0] }
    val storageSize : Int get() { return TownModel.townUpgrade[level][1] }


    val purchaseCost : Int
        get() {
            var idx = 0
            when ( _data.harbour ) {
                HarbourSize.pier -> idx = 0
                HarbourSize.docks -> idx = 1
                HarbourSize.marina -> idx = 2
            }
            return TownModel.townCost[idx]
        }

    val boats : ArrayList<BoatModel>
        get() { return _boats }

    fun setStorage(jobs: Iterable<JobModel?>) {
        if (jobs.count() <= _storage.size) {
            _storage.addAll(jobs)
            saveStorage()
        } else {
            assert(false)
        }
    }

    val upgradeCost : Int
        get() {
            var va = 0
            when ( level ) {
                1 -> va = purchaseCost / 2
                2 -> va = purchaseCost
                3 -> va = purchaseCost * 2
                else -> va = purchaseCost
            }
            return va
        }

    fun setStorage (jobs: List<JobModel>) {
        var idx = 0
        for (i in 0 until _storage.size) {
            if (idx >= jobs.size) { return }
            if (_storage[i] == null) {
                _storage[i] = jobs[idx]
                idx+=1
            }
        }
    }

    fun setJobs (jobs: List<JobModel>) {
        _jobs.addAll(jobs)
    }

    fun removeJob(job: JobModel) {
        _jobs.removeAll { it === job }
        if (job.isGold) {
            _data.startGold += job.value
        } else {
            _data.startSilver += job.value
        }
    }

    fun save() = runBlocking {
        Game.instance.db.townDao().update(_data)
    }

    fun saveJobs() = runBlocking {
        Game.instance.db.townJobDao().deleteByTownId(id)
        Game.instance.db.townJobDao().insert(_jobs.map { TownJobData(0, id, _data.jobsTimeStamp, it.data) })
    }

    fun saveStorage() = runBlocking {
        Game.instance.db.storageJobDao().deleteByTownId(id)
        Game.instance.db.storageJobDao().insert(_storage.mapNotNull{ if (it!=null) StorageJobData(0, id, it.data) else null })
    }

    private fun refreshJobs () {
        clearJobs()
        var unlockedTowns = ArrayList<TownModel>()
        Map.instance.towns.filterTo(unlockedTowns,{ it.level > 0 })
        unlockedTowns.removeAll { it === this }
        var numJobs = TownModel.townUpgrade[level][0]
        var jobTowns = ArrayList<TownModel>(unlockedTowns)
        unlockedTowns.forEach {
            for (x in 0 until it.level) {
                jobTowns.add(it)
            }
        }
        unlockedTowns.forEach {
            for (x in 0 until numJobs) {
                if (jobTowns.isEmpty()) { break }
                val roll = jobTowns.random()
                val job = JobModel(this,roll)
                jobTowns.remove(roll)
                _jobs.add(job)
            }
        }
        _data.jobsTimeStamp = User.instance.jobDate
        delegate?.jobsUpdated()
        save()
        saveJobs()
    }

    private fun clearJobs() {
        runBlocking {
            Game.instance.db.townJobDao().deleteByTownId(id)
        }
        _jobs.clear()
    }

    fun boatArrived (boat: BoatModel) {
        _boats.add(boat)
        _data.totalVisits += 1
        save()
    }

    fun boatDeparted (boat: BoatModel) {
        this._boats.removeAll { it === boat }
    }

    fun setup ( data: ArrayList<Any> ) {
        color =         Color.parseColor("#FF" + data[5] as String)
        _boats =        ArrayList<BoatModel>()
    }

    fun addBoat(boat: BoatModel) {
        _boats.add(boat)
    }
    fun removeBoat(boat: BoatModel) {
        _boats.removeAll{it === boat}
    }

    fun jobDelivered(job: JobModel) {
        if (job.isGold) {
            _data.endGold += job.value
        } else {
            _data.endSilver += job.value
        }
        save()
    }
}