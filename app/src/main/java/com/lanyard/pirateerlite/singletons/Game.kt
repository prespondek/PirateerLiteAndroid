package com.lanyard.pirateerlite.singletons

import android.content.Context
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import java.lang.ref.WeakReference
import com.lanyard.pirateerlite.models.GameModel
import java.util.*
import kotlin.collections.ArrayList

class Game private constructor(context: Context, mapConfig: HashMap<String, Any>) : GameModel(context, mapConfig), User.UserObserver {

    companion object {
        private lateinit var _instance: Game
        val instance: Game
            get() {
                return _instance
            }

        fun initialize(context: Context, mapConfig: HashMap<String, Any>) {
            synchronized(this) {
                _instance = Game(context, mapConfig)
            }
        }
    }

    override fun goldUpdated     (oldValue: Int, newValue: Int) {
        Audio.instance.queueSound("silver_large")
    }
    override fun silverUpdated   (oldValue: Int, newValue: Int) {
        Audio.instance.queueSound("silver_large")
    }

    interface GameListener {
        fun boatArrived(boat: BoatModel, town: TownModel) {}
        fun jobDelivered(boat: BoatModel, town: TownModel, gold: Int, silver: Int, quiet: Boolean) {}
        fun onDatabaseCreated () {}
    }

    private val _listeners: ArrayList<WeakReference<GameListener>>

    init {
        _listeners = ArrayList()
    }

    override fun databaseCreated() {
        clearDeadReferences()
        for (listener in _listeners) {
            listener.get()?.onDatabaseCreated()
        }
    }

    fun addGameListener(listener: GameListener) {
        _listeners.add(WeakReference(listener))
    }

    fun clearDeadReferences() {
        _listeners.removeAll { it.get() == null }
    }

    fun boatArrived(boat: BoatModel, town: TownModel) {
        clearDeadReferences()
        for (listener in _listeners) {
            listener.get()?.boatArrived(boat, town)
        }
    }

    fun jobDelivered(boat: BoatModel, town: TownModel, gold: Int, silver: Int, quiet: Boolean) {
        clearDeadReferences()
        for (listener in _listeners) {
            listener.get()?.jobDelivered(boat, town, gold, silver, quiet)
        }
    }

}