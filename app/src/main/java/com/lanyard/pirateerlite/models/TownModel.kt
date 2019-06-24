package com.lanyard.pirateerlite.models

import android.graphics.Color
import com.lanyard.pirateerlite.singletons.UserObserver
import com.lanyard.pirateerlite.singletons.User
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import com.lanyard.pirateerlite.singletons.Map

open class WorldNode {

}

interface TownDelegate {
    fun jobsUpdated ()
}



class TownModel (data: ArrayList<Any>): WorldNode(), UserObserver, Serializable {

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
        island, castle, pub, village, lighthouse, prison, fishermen, mansion, homestead
    }


    enum class HarbourSize {
        marina, docks, pier
    }
        /*static func <(left: TownModel.HarbourSize, right: TownModel.HarbourSize) -> Bool {
            return (left == .small && (right == .medium || right == .large)) ||
                    (left == .medium && right == .large)
        }
        static func >(left: TownModel.HarbourSize, right: TownModel.HarbourSize) -> Bool {
            return (left == .large && (right == .small || right == .medium)) ||
                    (left == .medium && right == .small)
        }
    }*/

    enum class Allegiance {
        none, british, american, spanish, french;

        companion object {
            val values = arrayOf(none,british, american, spanish, french)
        }
    }

    var type = TownType.island
    var allegiance = Allegiance.none
    var description = ""
    var name = ""
    var color = Color.BLACK
    var harbour = HarbourSize.pier
    var delegate : TownDelegate? = null
    private var _boats = ArrayList<BoatModel>()
    private var _jobs : ArrayList<JobModel>
    private var _storage : ArrayList<JobModel?>
    private var _jobsTimeStamp : Date
    private var _stats : TownStats

    var level : Int = 0
        set (value){
            if (value > TownModel.maxLevel) {
                field = TownModel.maxLevel
            } else {
                field = value
            }
            while (_storage.size < TownModel.townUpgrade[field][1]) {
                _storage.add(null)
            }
        }

    init {
        _jobs =         ArrayList<JobModel>()
        _storage =      ArrayList<JobModel?>()
        _jobsTimeStamp = Date()
        _stats = TownStats()
        level =         0
    }

    val storage : ArrayList<JobModel?>
        get() { return _storage }

    val stats : TownStats
        get() { return _stats }

    val jobs : ArrayList<JobModel>?
        get () {
            if (level == 0) {
                return null
            }
            if (_jobsTimeStamp != User.sharedInstance.jobDate) {
                refreshJobs()
            }
            return _jobs
        }

    val jobsDirty : Boolean
        get() { return _jobsTimeStamp != User.sharedInstance.jobDate }

    val jobsSize : Int get () { return TownModel.townUpgrade[level][0] }
    val storageSize : Int get() { return TownModel.townUpgrade[level][1] }


    val purchaseCost : Int
        get() {
            var idx = 0
            when ( harbour ) {
                HarbourSize.pier -> idx = 0
                HarbourSize.docks -> idx = 1
                HarbourSize.marina -> idx = 2
            }
            return TownModel.townCost[idx]
        }

    val boats : ArrayList<BoatModel>
        get() { return _boats }

    fun setStorage (jobs: ArrayList<JobModel?>) {
        if (jobs.size <= _storage.size) {
            _storage = jobs
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


    fun removeJob(job: JobModel) {
        _jobs.removeAll { it === job }
        if (job.isGold) {
            stats.startGold += job.value
        } else {
            stats.startSilver += job.value
        }
    }

    private fun refreshJobs () {
        _jobs.clear()
        var unlockedTowns = ArrayList<TownModel>()
        Map.sharedInstance.towns.filterTo(unlockedTowns,{ it.level > 0 })
        unlockedTowns.removeAll { it === this }
        var numJobs = TownModel.townUpgrade[level][0]
        unlockedTowns.forEach {
            for (x in 0..it.level - 1) {
                unlockedTowns.add(it)
            }
            if (numJobs > unlockedTowns.size) {
                numJobs = unlockedTowns.size
            }
            for (x in 0..numJobs - 1) {
                val roll = unlockedTowns.random()
                val job = JobModel(this,roll)
                _jobs.add(job)
            }
            _jobsTimeStamp = User.sharedInstance.jobDate
            delegate?.jobsUpdated()
        }
    }


    fun boatArrived (boat: BoatModel) {
        this._boats.add(boat)
        this._stats.totalVisits += 1
    }

    fun boatDeparted (boat: BoatModel) {
        this._boats.removeAll { it === boat }
    }


    fun setup ( data: ArrayList<Any> ) {
        name =          data[0] as String
        allegiance =    Allegiance.valueOf(data[1] as String)
        type =          TownType.valueOf(data[2] as String)
        description =   data[3] as String
        harbour =       HarbourSize.valueOf(data[4] as String)
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
            stats.endGold += job.value
        } else {
            stats.endSilver += job.value
        }
    }
}

class TownStats : Serializable
{
    var totalVisits : Int
    var startSilver : Int
    var endSilver : Int
    var startGold : Int
    var endGold : Int

    init {
        this.totalVisits = 0
        this.startSilver = 0
        this.endSilver = 0
        this.startGold = 0
        this.endGold = 0
    }
}