package com.lanyard.pirateerlite.models

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.internal.bind.ArrayTypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.lanyard.canvas.BitmapCache
import com.lanyard.pirateerlite.data.BoatData
import com.lanyard.pirateerlite.data.BoatJobData
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User
import kotlinx.coroutines.runBlocking
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BoatModel {
    enum class BoatIndex(val index: Int) {
        harbourType(0), distance(1), speed(2), frames(3), min_parts(4), max_parts(5),
        min_crew(6), max_crew(7), hold_width(8), hold_height(9), hold_size(10), parts(11),
        holdBG(12), title(13), image(14), level(15), part_amount(16), part_cost(17),
        upgrade_cost(18), boat_cost(19), description(20)
    }

    companion object {

        private lateinit var boatConfig: HashMap<String, Any>
        private lateinit var boatNames: ArrayList<ArrayList<Any>>
        var scale: Float = 1.0f
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

        fun initialize(context: Context) {
            val gson = Gson()
            var reader = JsonReader(
                InputStreamReader(context.assets.open("boat_model.json"))
            )
            boatConfig = gson.fromJson<HashMap<String, Any>>(reader, object : TypeToken<HashMap<String, Any>>() {}.type)
            reader = JsonReader(InputStreamReader(context.assets.open("boat_names.json")))
            boatNames = gson.fromJson<ArrayList<ArrayList<Any>>>(
                reader,
                object : TypeToken<ArrayList<ArrayList<Any>>>() {}.type
            )

            for (boat in boatKeys) {
                for (i in 1..9) {
                    BitmapCache.instance.addBitmap(
                        context,
                        boat + "_0" + i.toString() + ".png",
                        Bitmap.Config.ARGB_4444
                    )
                }
                for (i in 10..16) {
                    BitmapCache.instance.addBitmap(context, boat + "_" + i.toString() + ".png", Bitmap.Config.ARGB_4444)
                }
            }
        }

        val boatKeys: ArrayList<String>
            get() {
                return boatConfig["BoatTypes"] as ArrayList<String>
            }
        val boatValues: LinkedTreeMap<String, ArrayList<Any>>
            get() {
                return boatConfig["BoatData"] as LinkedTreeMap<String, ArrayList<Any>>
            }
        val boatParts: ArrayList<ArrayList<String>>
            get() {
                return boatConfig["BoatParts"] as ArrayList<ArrayList<String>>
            }

        fun boatConfig(type: String, index: BoatIndex): Any {
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

    private var _data: BoatData
    private var _course: MutableList<TownModel>
    private var _cargo: Array<JobModel?>
    private var _town: TownModel?

    val id get() =      _data.id
    val cargo get() =   _cargo
    val data get() =    _data
    val town get() =    _town
    val name get() =            _data.name
    val type get() =            _data.type
    val totalDistance get() = _data.totalDistance
    val totalSilver get() = _data.totalSilver
    val totalVoyages get() = _data.totalVoyages
    val courseTime get() = _data.courseTime
    val departureTime get() = _data.departureTime
    val cargoSize get() = _data.cargoSize
    val SPM: Double
        get() {
            return totalSilver.toDouble() / totalDistance
        }

    val arrivalTime: Long
        get() {
            return _data.departureTime + _data.courseTime
        }
    val remainingTime: Long
        get() {
            return arrivalTime - Date().time
        }

    val courseDistance: Double
        get() {
            return _data.courseTime.toDouble() * speed / 1000
        }
    val sailTime: Long
        get() {
            return Date().time - _data.departureTime
        }
    val course: List<TownModel>
        get() {
            return _course
        }
    val endurance: Double
        get() {
            return boatValues[_data.type]!![BoatIndex.distance.index] as Double
        }
    val speed: Double
        get() {
            return boatValues[_data.type]!![BoatIndex.speed.index] as Double
        }
    val percentCourseComplete: Float
        get() {
            return sailTime.toFloat() / _data.courseTime
        }
    val value: Int
        get() {
            return (boatValues[_data.type]!![BoatModel.BoatIndex.boat_cost.index] as Double).toInt()
        }
    val title: String
        get() {
            return boatValues[_data.type]!![BoatModel.BoatIndex.title.index] as String
        }

    fun plotCourse(town: TownModel) {
        _course.add(town)
    }

    fun plotCourse(towns: List<TownModel>) {
        _course.clear()
        _course.addAll(towns)
    }

    fun save() = runBlocking {
        Game.instance.db.boatDao().update(_data)
        Game.instance.db.boatJobDao().deleteByBoatId(id)
        Game.instance.db.boatJobDao().insert(_cargo.mapNotNull { if (it != null) { BoatJobData(0, id, it.data)} else null} )
    }

    fun setCargo(jobs: List<JobModel?>) {
        if (jobs.size <= _data.cargoSize) {
            _cargo = Array(_data.cargoSize, { jobs.elementAtOrNull(it) })
        } else {
            assert(false)
        }
        save()
    }

    constructor (type: String, name: String, town: TownModel) : this(
        BoatData(
            0,
            type,
            name,
            town.id,
            (boatConfig(type, BoatModel.BoatIndex.hold_size) as Double).toInt()
        )
    )

    constructor (data: BoatData) {
        this._data = data
        this._course = ArrayList<TownModel>()
        if (data.townid < 0) {
            this._town = null
            for (idx in _data.course) {
                _course.add(Map.instance._towns[idx])
            }
        } else {
            this._town = Map.instance._towns[data.townid]
            this._town?.addBoat(this)
        }
        this._cargo = Array<JobModel?>(_data.cargoSize, { null })
    }

    fun sail(distance: Float) {
        _data.departureTime = Date().time
        setCourseTime(distance)
        _data.totalDistance += distance.toDouble()
        town?.boatDeparted(this)
        _town = null
        _data.townid = -1
        _data.course = _course.map { it.id }.toTypedArray()
        save()
    }

    fun arrive(town: TownModel, quiet: Boolean = false) {
        var gold = 0.0
        var silver = 0.0
        var counter = 0

        fun rem(it: JobModel?): Boolean {
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
        for (i in 0 until _cargo.size) {
            if (rem(_cargo[i])) _cargo[i] = null
        }

        var multipler = 1.0
        if (counter == _data.cargoSize) {
            multipler = 1.25
        }
        if (quiet) {
            var user = User.instance
            User.instance.gold += (gold * multipler).toInt()
            User.instance.silver += (silver * multipler).toInt()
        } else {
            User.instance.addMoney((gold * multipler).toInt(), (silver * multipler).toInt())
        }
        _data.totalGold += gold.toInt()
        _data.totalSilver += silver.toInt()
        User.instance.xp += (silver * multipler).toInt()
        Game.instance.jobDelivered(this, town, gold.toInt(), silver.toInt(), quiet)
        /*val info = Intent("jobDelivered")
        info.putExtra("Town",town.name)
        info.putExtra("Silver",silver)
        info.putExtra("Gold",gold)
        info.putExtra("Boat", this.id)
        info.putExtra("Quiet", quiet)*/

        if (town === _course.last()) {
            _data.totalDistance = courseDistance
            _data.totalVoyages += 1
            _town = _course.last()
            _data.townid = town.id
            _town?.boatArrived(this)
            User.instance.boatArrived(this)
            _data.departureTime = 0
            _data.courseTime = 0
            _course.clear()
            //AudioManager.sharedInstance.playSound(sound: "boat_arrive")
            //NotificationCenter.default.post(name: NSNotification.Name.boatArrived, object: self, userInfo: ["Boat": self, "Town" : town])
            Game.instance.boatArrived(this, town)
            if (quiet == false) {
                User.instance.save()
                this.save()
                town.save()
            }
        }
    }

    val destination: TownModel?
        get() {
            return course.last()
        }

    val isMoored: Boolean
        get() {
            return this.town != null
        }


    fun setCourseTime(distance: Float) {
        _data.courseTime = getSailingTime(distance)
    }

    fun getSailingTime(distance: Float): Long {
        return (distance / speed * 1000).toLong()
    }

    // generates a random boat name using various methods
}
