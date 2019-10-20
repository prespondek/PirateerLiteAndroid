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

package com.lanyard.pirateerlite.singletons

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.lanyard.pirateerlite.models.BoatModel
import java.util.*
import kotlin.collections.HashMap
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.stream.JsonReader
import com.google.gson.reflect.TypeToken
import com.lanyard.pirateerlite.data.BoatData
import com.lanyard.pirateerlite.data.StatsData
import java.io.InputStreamReader
import java.lang.Math.random
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import com.lanyard.pirateerlite.data.UserData
import kotlinx.coroutines.runBlocking
import java.lang.NullPointerException


class User private constructor(userData: UserData, statData: Array<StatsData>, boatData: Array<BoatData>) {
    companion object {
        private var _user: User? = null

        private lateinit var userConfig: HashMap<String, Any>

        val boatStatInfo : Array<BoatStatInfo<*>> = arrayOf(
            BoatStatInfo("maxDistance", {boat-> boat.totalDistance}, {boat-> String.format("%.0fkm", boat.totalDistance / 10)}),
            BoatStatInfo("SPM", {boat-> boat.SPM}, {boat-> String.format("%.1f", boat.SPM * 10)}),
            BoatStatInfo("maxProfit",  {boat-> boat.totalSilver}),
            BoatStatInfo("maxVoyages", {boat-> boat.totalVoyages}))

        fun initialize(context: Context, userData: UserData, statData: Array<StatsData>, boatData: Array<BoatData>) {
            if (_user == null) {
                val gson = Gson()
                val reader = JsonReader(InputStreamReader(context.assets.open("user_model.json")))
                userConfig =
                    gson.fromJson<HashMap<String, Any>>(reader, object : TypeToken<HashMap<String, Any>>() {}.type)
                _user = User(userData, statData, boatData)
            }
            //_user?._data = data
        }

        val instance: User
            get() {
                check(_user != null) { "initialize User first" }
                return _user!!
            }

        val boatKeys: ArrayList<String>
            get() {
                return userConfig["Boats"] as ArrayList<String>
            }
        val rankKeys: ArrayList<String>
            get() {
                return userConfig["RankKeys"] as ArrayList<String>
            }
        val rankValues: LinkedTreeMap<String, ArrayList<Any>>
            get() {
                return userConfig["RankValues"] as LinkedTreeMap<String, ArrayList<Any>>
            }
        val exchangeRate: Int
            get() {
                return (userConfig["ExchangeRate"] as Double).toInt()
            }
        val jobInterval: Long
            get() {
                return (userConfig["JobTime"] as Double * 1000).toLong()
            }
        val marketInterval: Long
            get() {
                return (userConfig["MarketTime"] as Double * 1000).toLong()
            }
    }

    // Dumb shit because kotlin requires secondary constructors be called inline with the primary
    class  BoatStatInfo <out T : Number> {
        val statName : String
        val statData : (BoatModel) -> T
        val statString: (BoatModel) -> String get() = _statString
        val statComp: (BoatModel,BoatModel) -> Boolean get() = _statComp

        private lateinit var _statString: (BoatModel) -> String
        private lateinit var _statComp: (BoatModel,BoatModel) -> Boolean

        constructor(statName : String,
                    statData : (BoatModel) -> T,
                    statString: (BoatModel) -> String,
                    statComp: (BoatModel,BoatModel) -> Boolean) {
            this.statName = statName
            this.statData = statData
            this._statComp = statComp
            this._statString = statString
        }
        constructor(statName : String,
                    statData : (BoatModel) -> T) {
            this.statName = statName
            this.statData = statData
            delegateConstructor()
        }

        constructor(statName : String,
                    statData : (BoatModel) -> T,
                    statString: (BoatModel) -> String) {
            this.statName = statName
            this.statData = statData
            this._statString = statString
            delegateConstructor()
        }

        fun delegateConstructor () {
            if (!this::_statComp.isInitialized) {
                this._statComp = {boat1,boat2-> this.statData(boat1).toDouble() > this.statData(boat2).toDouble()}
            }
            if (!this::_statString.isInitialized) {
                this._statString = {boat-> statData(boat).toString()}
            }
        }

    }

    interface UserListener {
        fun goldUpdated     (oldValue: Int, newValue: Int) {}
        fun silverUpdated   (oldValue: Int, newValue: Int) {}
        fun xpUpdated       (oldValue: Int, newValue: Int) {}
        fun boatAdded       (boat: BoatModel) {}
        fun boatRemoved     (boat: BoatModel) {}
        fun statsUpdated    () {}
    }

    enum class MarketItem(val index: Int) {
        hull    (0),
        rigging (1),
        sails   (2),
        cannon  (3),
        boat    (4);

        companion object {
            fun withIndex(idx: Int): MarketItem {
                val arr = arrayOf(hull, rigging, sails, cannon, boat)
                return arr[idx]
            }
        }
    }

    class BoatPart(val boat: String, val item: MarketItem) : Parcelable {

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.writeString(boat)
            dest?.writeInt(item.index)
        }

        constructor(parcel: Parcel) : this(
            parcel.readString(),
            MarketItem.withIndex(parcel.readInt()))

        override fun describeContents(): Int {
            return 0
        }

        override fun equals(other: Any?): Boolean {
            var rhs = other as? BoatPart
            if (rhs == null) return false
            return boat == rhs.boat && item == rhs.item
        }

        companion object CREATOR : Parcelable.Creator<BoatPart> {
            override fun createFromParcel(parcel: Parcel): BoatPart {
                return BoatPart(parcel)
            }

            override fun newArray(size: Int): Array<BoatPart?> {
                return arrayOfNulls(size)
            }
        }
    }

    private var _market: ArrayList<BoatPart>
    private var _stats: MutableList<StatsData>
    private var _data : UserData
    private var _boatModels: ArrayList<BoatModel>
    private var _listeners = ArrayList<WeakReference<UserListener>>()

    init {
        this._data = userData
        this._stats = statData.toMutableList()
        this._market = ArrayList<BoatPart>()
        val map = Map.instance
        _boatModels = ArrayList<BoatModel>()
        this.updateMarket()
        for (boat in boatData) {
            _boatModels.add(BoatModel(boat))
        }
    }

    var gold: Int
        get() = _data.gold
        set (value) {
            var oldvalue = _data.gold
            _data.gold = value
            if (oldvalue != _data.gold) {
                cleanObservers()
                for (container in _listeners) {
                    container.get()?.goldUpdated(oldvalue, _data.gold)
                }
            }
        }

    var silver: Int
        get() = _data.silver
        set (value) {
            var oldvalue = _data.silver
            _data.silver = value
            if (oldvalue != _data.silver) {
                cleanObservers()
                for (container in _listeners) {
                    container.get()?.silverUpdated(oldvalue, _data.silver)
                }
            }
        }
    var xp: Int
        get() = _data.xp
        set (value) {
            var oldvalue = _data.xp
            _data.xp = value
            cleanObservers()
            for (container in _listeners) {
                container.get()?.xpUpdated(oldvalue, silver)
            }
            if (levelForXp(oldvalue) != levelForXp(_data.xp)) {
                /*val alert = UIAlertController("Level Up", "Newer boats are available for you to build. Shipyard and market have been updated." , UIAlertController.Style.alert)
                alert.addAction(UIAlertAction("Continue", .default, null))
                AlertQueue.shared.pushAlert(alert, onPresent: {
                    AudioManager.sharedInstance.playSound(sound: "level_up")
                })*/
            }
        }

    val boatsSold get() = _data.boatsSold
    val distance  get() = _data.distance
    val voyages   get() = _data.voyages
    val stats     get() = _stats
    val parts     get() = _data.parts

    var boatSlots: Int
        get() = _data.boatSlots
        set (value) { _data.boatSlots = value }

    fun save() = runBlocking {
        Game.instance.db.userDao().update(_data)
    }

    val levelXp: Int
        get() {
            return xpForLevel(this.level)
        }

    val startDate: Date
        get() {
            return _data.startDate
        }

    fun xpForLevel(level: Int): Int {
        return (level + 1).toDouble().pow(2).toInt() * 1500
    }

    val level: Int
        get() {
            return levelForXp(this.xp)
        }

    fun levelForXp(xp: Int): Int {
        if (xp == 0) {
            return 0
        }
        return max(1, log2(xp.toFloat() / 1500).toInt())
    }


    fun statsUpdated() {
        cleanObservers()
        for (container in _listeners) {
            container.get()?.statsUpdated()
        }
    }

    fun addMoney(gold: Int, silver: Int) {
        this.gold += gold
        this.silver += silver
        statsUpdated()
    }


    val boatSlotCost: Int
        get() {
            var va = 1024
            for (x in 0 until _data.boatSlots) {
                va += va / 2
            }
            return va
        }

    fun boatArrived(boat: BoatModel) {
        _data.distance += boat.courseDistance
        _data.time += boat.courseTime
        _data.voyages += 1

        for (stat in boatStatInfo) {
            var statData = getStat(stat.statName, boat)
            if (statData != null && stat.statComp(boat,statData.second)) {
                statData.first.boatData = boat.data
                runBlocking {
                    Game.instance.db.statsDao().update(statData.first)
                }
            }
        }
        statsUpdated()
    }

    fun getStat(stat: String, default: BoatModel? = null) : Pair<StatsData,BoatModel>? {
        var statData = _stats.find { it.stat == stat }
        if ( statData == null ) {
            if ( default != null ) {
                statData = StatsData(stat, default.data)
                _stats.add(statData)
                runBlocking {
                    Game.instance.db.statsDao().insert(statData)
                }
            } else {
                return null
            }
        }
        var statBoat = boats.find { it.id == statData.boatData.id } ?: throw NullPointerException()
        return Pair<StatsData,BoatModel>(statData, statBoat)
    }

    fun addBoat(boat: BoatModel) {
        _boatModels.add(boat)
        runBlocking {
            boat.data.id = Game.instance.db.boatDao().insert(boat.data)
        }
        cleanObservers()
        for (observer in _listeners) {
            observer.get()?.boatAdded(boat)
        }
    }

    fun canBuildBoat(type: String): Boolean {
        var parts = BoatModel.boatConfig(type, BoatModel.BoatIndex.part_amount) as List<Int>
        for (i in 0 until parts.size) {
            val currPart = BoatPart(type, MarketItem.withIndex(i))
            val tparts = User.instance.parts.filter { it == currPart }
            val numParts = tparts.size
            val targetParts = parts[i]
            if (numParts < targetParts) {
                return false
            }
        }
        return true
    }

    private fun cleanObservers() {
        _listeners.removeAll { it.get() == null }
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
            var diff = Date().time - _data.marketDate.time
            diff /= marketInterval
            _data.marketDate = Date(_data.marketDate.time + diff * User.marketInterval)
            return _data.marketDate
        }

    val jobDate: Date
        get() {
            var diff = Date().time - _data.jobDate.time
            diff /= jobInterval
            _data.jobDate = Date(_data.jobDate.time + diff * User.jobInterval)
            return _data.jobDate
        }

    val market: ArrayList<BoatPart>
        get() {
            val date = marketDate
            if (_data.marketDate != date) {
                updateMarket()
                _data.marketDate = date
            }
            return _market
        }

    fun removeBoat(boat: BoatModel) {
        if (boat.town != null) {
            boat.town!!.removeBoat(boat)
        }
        _boatModels.removeAll { it === boat }
        runBlocking {
            Game.instance.db.boatDao().delete(boat.data)
        }
        /*for (idx in 0 until _boatModels.size) {
            _boatModels[idx].id = idx
        }*/
        cleanObservers()
        for (observer in _listeners) {
            observer.get()?.boatRemoved(boat)
        }
        _data.boatsSold += 1
        statsUpdated()
    }

    val boats: ArrayList<BoatModel>
        get() {
            return _boatModels
        }

    fun purchaseBoatWithParts(boat: BoatModel, parts: List<BoatPart>) {
        for (part in parts) {
            this.parts.remove(this.parts.last { it == part })
        }
        addBoat(boat)
    }

    fun purchaseBoatWithMoney(boat: BoatModel, parts: List<BoatPart>) {
        addMoney(gold - (BoatModel.boatConfig(boat.type, BoatModel.BoatIndex.boat_cost) as Int), 0)
        for (part in parts) {
            this._market.remove(this._market.last { it == part })
        }
        addBoat(boat)
    }

    fun addListerner(observer: UserListener) {
        _listeners.add(WeakReference(observer))
    }

    fun removeListener(observer: UserListener) {
        _listeners.removeAll { it.get() === observer }
    }

    private fun updateMarket() {
        _market.clear()
        for (key in BoatModel.boatKeys) {
            val level = BoatModel.boatConfig(key, BoatModel.BoatIndex.level) as String
            if (this.level < User.rankKeys.indexOfFirst { level == it }) {
                continue
            }
            for (i in 0 until 4) {
                val parts = BoatModel.boatConfig(key, BoatModel.BoatIndex.part_amount) as ArrayList<Int>

                if (random().roundToInt() == 0) {
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
