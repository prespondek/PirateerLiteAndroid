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
        fun boatSailed(boat: BoatModel) {}
        fun boatArrived(boat: BoatModel, town: TownModel) {}
        fun jobDelivered(boat: BoatModel, town: TownModel, gold: Int, silver: Int, quiet: Boolean) {}
        fun boatJobsChanged(boat: BoatModel) {}
        fun onDatabaseCreated () {}
    }

    private val _listeners: ArrayList<WeakReference<GameListener>>

    init {
        _listeners = ArrayList()
    }

    fun addGameListener(listener: GameListener) {
        _listeners.add(WeakReference(listener))
    }

    fun removeGameListener(listener: GameListener) {
        _listeners.removeAll { it.get() == listener }
    }

    fun clearDeadReferences() {
        _listeners.removeAll { it.get() == null }
    }

    fun callListeners (exec: (GameListener)->Unit) {
        clearDeadReferences()
        for (listener in _listeners) {
            exec(listener.get()!!)
        }
    }

    override fun databaseCreated() {
        callListeners { it.onDatabaseCreated() }
    }

    fun boatArrived(boat: BoatModel, town: TownModel) {
        callListeners { it.boatArrived(boat, town) }
    }

    fun boatJobsChanged(boat: BoatModel) {
        callListeners { it.boatJobsChanged(boat) }
    }

    fun jobDelivered(boat: BoatModel, town: TownModel, gold: Int, silver: Int, quiet: Boolean) {
        callListeners { it.jobDelivered(boat, town, gold, silver, quiet) }
    }

    fun boatSailed(boat: BoatModel) {
        callListeners { it.boatSailed(boat) }
    }
}