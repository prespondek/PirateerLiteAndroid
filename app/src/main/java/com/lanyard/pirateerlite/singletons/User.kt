package com.lanyard.pirateerlite.singletons

import com.lanyard.pirateerlite.models.BoatModel
import java.util.*
import kotlin.collections.HashMap
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.stream.JsonReader
import com.google.gson.reflect.TypeToken
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.models.BoatStats
import java.io.InputStreamReader
import java.lang.Math.pow
import java.lang.Math.random
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

interface UserObserver {
    fun goldUpdated(oldValue: Int, newValue: Int) {}
    fun silverUpdated(oldValue: Int, newValue: Int) {}
    fun xpUpdated(oldValue: Int, newValue: Int) {}
    fun boatAdded(boat: BoatModel) {}
    fun boatRemoved(boat: BoatModel) {}
    fun statsUpdated() {}
}

class User private constructor() {
    companion object {
        private var _user: User? = null

        val sharedInstance: User
            get() {
                if (_user != null) {
                    return _user!!
                } else {


                }
                _user = User()
                return _user!!
            }

        private val userData: HashMap<String, Any>

        init {
            val gson = Gson()
            val reader = JsonReader(InputStreamReader(MapActivity.instance.assets.open("user_model.json")))
            userData = gson.fromJson<HashMap<String, Any>>(reader, object : TypeToken<HashMap<String, Any>>() {}.type)
        }

        val boatKeys: ArrayList<String>
            get() {
                return userData["Boats"] as ArrayList<String>
            }
        val rankKeys: ArrayList<String>
            get() {
                return userData["RankKeys"] as ArrayList<String>
            }
        val rankValues: LinkedTreeMap<String, ArrayList<Any>>
            get() {
                return userData["RankValues"] as LinkedTreeMap<String, ArrayList<Any>>
            }
        val exchangeRate: Int
            get() {
                return userData["ExchangeRate"] as Int
            }
        val jobInterval: Long
            get() {
                return userData["JobTime"] as Long
            }
        val marketInterval: Long
            get() {
                return userData["MarketTime"] as Long
            }
    }
        enum class MarketItem(val index: Int) {
            hull(0),
            rigging(1),
            sails(2),
            cannon(3),
            boat(4);

            companion object {
                fun withIndex(idx: Int): MarketItem {
                    val arr = arrayOf(hull, rigging, sails, cannon, boat)
                    return arr[idx]
                }
            }
        }

        class Stats {
            var distance: Double = 0.0
            var voyages: Int = 0
            var silver: Int = 0
            var gold: Int = 0
            var time: Long = 0
            var boatsSold: Int = 0
            var boatStats = HashMap<String, BoatArchive>()

            init {
                boatStats["maxDistance"] = BoatArchive()
                boatStats["maxProfit"] = BoatArchive()
                boatStats["SPM"] = BoatArchive()
                boatStats["maxVoyages"] = BoatArchive()
            }
        }

        class BoatArchive {
            var name: String
            var type: String
            var stats: BoatStats

            constructor() {
                name = "---"
                type = ""
                stats = BoatStats()
            }

            constructor (boat: BoatModel) {
                name = boat.name
                type = boat.type
                stats = boat.stats
            }
        }

        class BoatPart(boat: String, item: MarketItem) {
            var boat: String
            var item: MarketItem

            init {
                this.boat = boat
                this.item = item
            }

            override fun equals(other: Any?): Boolean {
                var rhs = other as? BoatPart
                if (rhs == null) return false
                return boat == rhs.boat && item == rhs.item
            }
        }

        var parts: ArrayList<BoatPart>
        private var _market: ArrayList<BoatPart>
        var boatSlots: Int
        private var _stats: Stats
        private var _boatModels: ArrayList<BoatModel>
        private var _observers = ArrayList<UserObserver>()
        private var _marketDate: Date
        private var _jobDate: Date
        private var _startDate: Date

        var gold: Int = 0
            set (value) {
                var oldvalue = field
                field = value
                if (oldvalue != field) {
                    for (container in _observers) {
                        container.goldUpdated(oldvalue, field)
                    }
                }
            }

        var silver: Int = 0
            set (value) {
                var oldvalue = field
                field = value
                if (oldvalue != field) {
                    for (container in _observers) {
                        container.silverUpdated(oldvalue, field)
                    }
                }
            }
        var xp: Int = 0
            set (value) {
                var oldvalue = field
                field = value
                for (container in _observers) {
                    container.xpUpdated(oldvalue, silver)
                }
                if (levelForXp(oldvalue) != levelForXp(field)) {
                    /*val alert = UIAlertController("Level Up", "Newer boats are available for you to build. Shipyard and market have been updated." , UIAlertController.Style.alert)
                    alert.addAction(UIAlertAction("Continue", .default, null))
                    AlertQueue.shared.pushAlert(alert, onPresent: {
                        AudioManager.sharedInstance.playSound(sound: "level_up")
                    })*/
                }
            }

        init {
            this._stats = Stats()
            this.gold = 8
            this.silver = 4000
            this.xp = 0
            this.boatSlots = 4
            this.parts = ArrayList<BoatPart>()
            this._market = ArrayList<BoatPart>()
            this._marketDate = Date()
            this._jobDate = Date()
            this._startDate = Date()
            val map = Map.sharedInstance
            _boatModels = ArrayList<BoatModel>()
            this.updateMarket()
            var boat = BoatModel("raft", BoatModel.makeName(), map.towns[1])
            _boatModels.add(boat)
            boat = BoatModel("raft", BoatModel.makeName(), map.towns[4])
            _boatModels.add(boat)
            boat = BoatModel("skiff", BoatModel.makeName(), map.towns[1])
            _boatModels.add(boat)
            map.towns[1].level = 1
            map.towns[4].level = 1
            map.towns[5].level = 1
            //map.towns[9].level = 1
            map.towns[43].level = 1
        }

        fun save() {
        }

        val levelXp: Int
            get() {
                return xpForLevel(this.level)
            }

        val startDate: Date
            get() {
                return _startDate
            }

        fun xpForLevel(level: Int): Int {
            return (pow(2.0, ((level + 1) * 1500.0))).toInt()
        }

        val level: Int
            get() {
                return levelForXp(this.xp)
            }

        fun levelForXp(xp: Int): Int {
            return (max(0.0f, log2(xp.toFloat() / 1500))).toInt()
        }


        fun statsUpdated() {
            for (container in _observers) {
                container.statsUpdated()
            }
        }

        fun addMoney(gold: Int, silver: Int) {
            this.gold += gold
            this.silver += silver
            if (gold != 0 || silver != 0) {
                //AudioManager.sharedInstance.playSound(sound:"silver_large")
            }
            stats.silver += silver
            stats.gold += gold
            statsUpdated()
        }


        val boatSlotCost: Int
            get() {
                var va = 1024
                for (x in 0..boatSlots - 1) {
                    va += va / 2
                }
                return va
            }

        val stats: Stats
            get() {
                return _stats
            }

        fun boatArrived(boat: BoatModel) {
            stats.distance += boat.courseDistance
            stats.time += boat.courseTime
            stats.voyages += 1
            if (boat.stats.totalDistance > stats.boatStats["maxDistance"]!!.stats.totalDistance) {
                stats.boatStats["maxDistance"] = BoatArchive(boat)
            }
            if (boat.stats.SPM > stats.boatStats["SPM"]!!.stats.SPM) {
                stats.boatStats["SPM"] = BoatArchive(boat)
            }
            if (boat.stats.totalSilver > stats.boatStats["maxProfit"]!!.stats.totalSilver) {
                stats.boatStats["maxProfit"] = BoatArchive(boat)
            }
            if (boat.stats.totalVoyages > stats.boatStats["maxVoyages"]!!.stats.totalVoyages) {
                stats.boatStats["maxVoyages"] = BoatArchive(boat)
            }
            statsUpdated()
        }

        fun addBoat(boat: BoatModel) {
            _boatModels.add(boat)
            for (observer in _observers) {
                observer.boatAdded(boat)
            }
        }

        fun canBuildBoat(type: String): Boolean {
            var parts = BoatModel.boatData(type, BoatModel.BoatIndex.part_amount) as Array<Int>
            for (i in 0..parts.size - 1) {
                val currPart = BoatPart(type, i as MarketItem)
                val tparts = User.sharedInstance.parts.filter { it == currPart }
                val numParts = tparts.size
                val targetParts = parts[i]
                if (numParts < targetParts) {
                    return false
                }
            }
            return true
        }

        fun boatAtIndex(idx: Int): BoatModel? {
            if (_boatModels.size > idx) {
                return _boatModels[idx]
            }
            return null
        }

        val numBoats: Int
            get() {
                return _boatModels.size
            }

        val marketDate: Date
            get() {
                var diff = Date().time - _marketDate.time
                diff /= User.marketInterval
                return Date(_marketDate.time + diff * User.marketInterval)
            }

        val jobDate: Date
            get() {
                var diff = Date().time - _jobDate.time
                diff /= User.jobInterval
                _jobDate = Date(_jobDate.time + diff * User.jobInterval)
                return _jobDate
            }

        val market: ArrayList<BoatPart>
        get(){
            val date = marketDate
            if (_marketDate != date) {
                updateMarket()
                _marketDate = date
            }
            return _market
        }

        fun removeBoat(boat: BoatModel) {
            if (boat.town != null) {
                boat.town!!.removeBoat(boat)
            }
            _boatModels.removeAll { it === boat }
            for (observer in _observers) {
                observer.boatRemoved(boat)
            }
            stats.boatsSold += 1
            statsUpdated()
        }

        val boats: ArrayList<BoatModel>
        get(){
            return _boatModels
        }

        fun purchaseBoatWithParts(boat: BoatModel, parts: List<BoatPart>) {
            for (part in parts) {
                this.parts.removeAll { it === part }
            }
            addBoat(boat)
        }

        fun purchaseBoatWithMoney(boat: BoatModel, parts: List<BoatPart>) {
            addMoney(gold - (BoatModel.boatData(boat.type, BoatModel.BoatIndex.boat_cost) as Int), 0)
            for (part in parts) {
                this._market.removeAll { it === part }
            }
            addBoat(boat)
        }

        fun removePart(part: BoatPart) {
            val idx = _market.indexOfFirst { it === part }
            if (idx != null) {
                _market.removeAt(idx)
            }
        }


        private fun updateMarket() {
            _market.clear()
            for (key in BoatModel.boatKeys) {
                val level = BoatModel.boatData(key, BoatModel.BoatIndex.level) as String
                if (this.level < User.rankKeys.indexOfFirst{ level == it}) { continue }
                for (i in 0..4) {
                    val parts = BoatModel.boatData(key, BoatModel.BoatIndex.part_amount) as ArrayList<Int>

                    if (random().roundToInt()  == 0) {
                        if (i < 4) {
                            val num_parts = parts[i]
                            if (num_parts == 0) {
                                continue
                            }
                        }
                        val part = BoatPart(key, MarketItem.withIndex(i))
                        _market.add(part)
                    }
                }
            }
        }
    }
