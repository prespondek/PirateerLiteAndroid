package com.lanyard.pirateerlite.models

import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.lanyard.canvas.BitmapCache
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.singletons.User
import java.io.InputStreamReader
import java.lang.Math.max
import java.util.*
import kotlin.collections.HashMap

class BoatModel(type: String, name: String) {
    enum class BoatIndex(val index: Int) {
        harbourType(0), distance(1), speed(2), frames(3), min_parts(4), max_parts(5),
        min_crew(6), max_crew(7), hold_width(8), hold_height(9), hold_size(10), parts(11),
        holdBG(12), title(13), image(14), level(15), part_amount(16), part_cost(17),
        upgrade_cost(18), boat_cost(19), description(20)
    }

    companion object {

        private val boatData: HashMap<String, Any>
        private val boatNames: ArrayList<ArrayList<Any>>
        var scale : Float = 1.0f
        set(value) {
            for (i in boatValues) {
                var distance = i.value[BoatIndex.distance.index] as Double
                distance = distance / field * value
                i.value[BoatIndex.distance.index] = distance
                var speed = i.value[BoatIndex.speed.index] as Double
                speed = speed / field * value
                i.value[BoatIndex.speed.index] = speed
            }
            field = value
        }

        init {
            val gson = Gson()
            var reader = JsonReader(
                InputStreamReader (MapActivity.instance.assets.open("boat_model.json")))
            boatData = gson.fromJson<HashMap<String, Any>>(reader, object : TypeToken<HashMap<String, Any>>() {}.type)
            reader = JsonReader(InputStreamReader (MapActivity.instance.assets.open("boat_names.json")))
            boatNames = gson.fromJson<ArrayList<ArrayList<Any>>>(reader, object : TypeToken<ArrayList<ArrayList<Any>>>() {}.type)

            for (boat in boatKeys) {
                for(i in 1..9) {
                    BitmapCache.instance.addBitmap( boat + "_0" + i.toString() + ".png",Bitmap.Config.ARGB_4444)
                }
                for(i in 10..16) {
                    BitmapCache.instance.addBitmap(boat + "_" + i.toString() + ".png",Bitmap.Config.ARGB_4444)
                }
            }
        }

        val boatKeys: ArrayList<String>
            get() {
                return boatData["BoatTypes"] as ArrayList<String>
            }
        val boatValues: LinkedTreeMap<String, ArrayList<Any>>
            get() {
                return boatData["BoatData"] as LinkedTreeMap<String, ArrayList<Any>>
            }
        val boatParts: ArrayList<ArrayList<String>>
            get() {
                return boatData["BoatParts"] as ArrayList<ArrayList<String>>
            }

        fun boatData(type: String, index: BoatIndex): Any {
            return boatValues[type]!![index.index]
        }

        fun makeName(): String {
            var name = String()
            val type = (Math.random() * 5 + 1).toInt()

            for (frag in boatNames) {
                val name_frag = frag.first() as String
                val frag_values = frag.last() as ArrayList<String>
                val idx = (Math.random() * frag_values.size - 1).toInt()
                if (name_frag == "ProNoun" && (type == 5 || type == 2 || type == 3)) {
                    name += frag_values[idx] + " "
                } else if (name_frag == "Owner" && (type == 4 || type == 2 || type == 6)) {
                    name += frag_values[idx]
                    if (type == 4) {
                        name += "'s"
                    }
                    name += " "

                } else if (name_frag == "Verb" && type == 1) {
                    name += frag_values[idx] + " "
                } else if (name_frag == "Subject" && (type == 1 || type == 3 || type == 4 || type == 5)) {
                    name += frag_values[idx] + " "
                } else if (name_frag == "Location" && type == 6) {
                    name += frag_values[idx] + " "
                }
            }
            return name
        }
    }

    val type: String
    private var _departureTime: Long
    private var _courseTime: Long
    private var _cargo: MutableList<JobModel?>
    private var _name: String = ""
    private var _speed: Float = 0.0f
    private var _town: TownModel? = null
    private var _course: MutableList<TownModel>
    private var _cargoSize: Int = 0
    private var _stats: BoatStats

    val stats: BoatStats
        get() {
            return _stats
        }
    val name: String
        get() {
            return _name
        }

    val cargo: List<JobModel?>
        get() {
            return _cargo
        }

    val cargoSize: Int
        get() {
            return _cargoSize
        }
    val arrivalTime: Long
        get() {
            return _departureTime + _courseTime
        }
    val remainingTime: Long
        get() {
            return arrivalTime - Date().time
        }
    val departureTime: Long
        get() {
            return _departureTime
        }
    val courseTime: Long
        get() {
            return _courseTime
        }
    val courseDistance: Double
        get() {
            return _courseTime.toDouble() * _speed.toDouble()
        }
    val sailTime: Long
        get() {
            return Date().time - _departureTime
        }
    val town: TownModel?
        get() {
            return _town
        }
    val course: List<TownModel>
        get() {
            return _course
        }
    val endurance: Double
        get() {
            return boatValues[this.type]!![1] as Double
        }
    val percentCourseComplete: Float
        get() {
            return sailTime.toFloat() / _courseTime
        }
    val value: Int
        get() {
            return boatValues[this.type]!![BoatModel.BoatIndex.boat_cost.index] as Int
        }
    val title: String
        get() {
            return boatValues[this.type]!![BoatModel.BoatIndex.title.index] as String
        }

    fun plotCourse(town: TownModel) {
        _course.add(town)
    }

    fun plotCourse(towns: List<TownModel>) {
        _course.clear()
        _course.addAll(towns)
    }

    fun setCargo(jobs: List<JobModel?>) {
        if (jobs.size <= _cargoSize) {
            _cargo.clear()
            _cargo.addAll(jobs)
        } else {
            assert(false)
        }
    }

    constructor(type: String, name: String, town: TownModel) : this(type, name) {
        this._town = town
        this._town?.addBoat(this)
    }

    init {
        this._departureTime = 0
        this._courseTime = 0
        this._name = name
        this.type = type
        this._cargo = ArrayList<JobModel?>()
        this._course = ArrayList<TownModel>()
        var data = boatValues[this.type]!!
        this._speed = (data[2] as Double).toFloat() * scale
        this._cargoSize = (data[10] as Double).toInt()
        this._stats = BoatStats()
    }

    fun sail(distance: Float) {
        this._departureTime = Date().time
        setDistance(distance)
        this._stats.totalDistance += distance.toDouble()
        this._town?.boatDeparted(this)
        this._town = null
    }

    fun arrive(town: TownModel, quiet: Boolean = false) {
        var gold = 0.0
        var silver = 0.0
        var counter = 0

        fun rem (it: JobModel?) : Boolean {
            if (it != null && town === it.destination) {
                counter += 1
                if (it.isGold) {
                    gold += it.value
                } else {
                    silver += it.value
                }
                town.jobDelivered(it)
                return true
            }
            return false
        }
        _cargo.removeAll { rem(it) }

        var multipler = 1.0
        if (counter == cargoSize) {
            multipler = 1.25
        }
        if (quiet) {
            var user = User.sharedInstance
            User.sharedInstance.gold += (gold * multipler).toInt()
            User.sharedInstance.silver += (silver * multipler).toInt()
        } else {
            User.sharedInstance.addMoney((gold * multipler).toInt(), (silver * multipler).toInt())
        }
        this._stats.totalGold += gold.toInt()
        this._stats.totalSilver += silver.toInt()
        User.sharedInstance.xp += (silver * multipler).toInt()
        //NotificationCenter.default.post(name: NSNotification.Name.jobDelivered, object: self, userInfo: ["Town" : town, "Silver": Int(silver), "Gold": Int(gold), "Boat": self, "Quiet": quiet])

        if (town === _course.last()) {
            this._stats.totalDistance = courseDistance
            this._stats.totalVoyages += 1
            this._town = _course.last()
            this._town?.boatArrived(this)
            User.sharedInstance.boatArrived(this)
            this._departureTime = 0
            this._courseTime = 0
            _course.clear()
            //AudioManager.sharedInstance.playSound(sound: "boat_arrive")
            //NotificationCenter.default.post(name: NSNotification.Name.boatArrived, object: self, userInfo: ["Boat": self, "Town" : town])
            if (quiet == false) {
                User.sharedInstance.save()
            }
        }
    }

    val destination: TownModel?
        get() {
            return course.last()
        }

    val isMoored: Boolean
        get() {
            return this._town != null
        }


    fun setDistance(distance: Float) {
        this._courseTime = getSailingTime(distance)
    }

    fun getSailingTime(distance: Float): Long {
        return (distance / this._speed).toLong()
    }

    // generates a random boat name using various methods
}

class BoatStats {
    var totalDistance: Double
    var totalVoyages: Int
    var totalSilver: Int
    var totalGold: Int
    var datePurchased: Date

    val SPM: Double
        get() {
            return max(0.0, (totalSilver / totalDistance).toDouble())
        }

    init {
        this.totalSilver = 0
        this.totalGold = 0
        this.totalDistance = 0.0
        this.datePurchased = Date()
        this.totalVoyages = 0
    }
}