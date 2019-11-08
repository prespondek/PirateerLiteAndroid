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

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lanyard.pirateerlite.data.BoatData
import com.lanyard.pirateerlite.data.GameDatabase
import com.lanyard.pirateerlite.data.TownData
import com.lanyard.pirateerlite.data.UserData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

abstract class GameModel(context: Context, mapConfig: HashMap<String, Any>) {
    private val dbCreated: MutableLiveData<Boolean>

    @Volatile
    var db: GameDatabase
        private set

    inner class dbCallback(val mapConfig: HashMap<String, Any>) : RoomDatabase.Callback() {
        var createDb = false

        override fun onCreate(sqldb: SupportSQLiteDatabase) {
            super.onCreate(sqldb)
            createDb = true
            GlobalScope.launch {
                    db.userDao().insert(UserData())
                val townConfig = mapConfig["TownInfo"] as ArrayList<ArrayList<Any>>
                    for (i in 0 until townConfig.size) {
                        val townInfo = townConfig[i]
                        val name = townInfo[0] as String
                        val allegiance = TownModel.Allegiance.valueOf(townInfo[1] as String)
                        val type = TownModel.TownType.valueOf(townInfo[2] as String)
                        val description = townInfo[3] as String
                        val harbour = TownModel.HarbourSize.valueOf(townInfo[4] as String)
                        var level = 0
                        if (i == 1 || i == 4 || i == 5) {
                            level = 1
                        }
                        db.townDao().insert(
                            TownData(i.toLong(), name, type, allegiance, harbour, description, level)
                        )
                    }
                    db.boatDao().insert(
                        BoatData(0, "raft", BoatModel.makeName(), 1,
                            (BoatModel.boatConfig("raft", BoatModel.BoatIndex.hold_size) as Double).toInt()
                        )
                    )
                    db.boatDao().insert(
                        BoatData(0, "raft", BoatModel.makeName(), 4,
                            (BoatModel.boatConfig("raft", BoatModel.BoatIndex.hold_size) as Double).toInt()
                        )
                    )
                    db.boatDao().insert(
                        BoatData(0, "skiff", BoatModel.makeName(), 1,
                            (BoatModel.boatConfig("skiff", BoatModel.BoatIndex.hold_size) as Double).toInt()
                        )
                    )
                    dbCreated.postValue(true)
                }

        }

        override fun onOpen(sqldb: SupportSQLiteDatabase) {
            super.onOpen(sqldb)
            if (createDb == false) {
                dbCreated.postValue(true)
            }
        }
    }

    abstract fun databaseCreated()

    init {
        dbCreated = MutableLiveData<Boolean>()

        var observer : Observer<Boolean>? = null
        observer = object : Observer<Boolean> {
            override fun onChanged(t: Boolean) {
                dbCreated.removeObserver(observer!!)
                databaseCreated()
            }
        }
        dbCreated.observeForever(observer)
        db = Room.databaseBuilder(
            context,
            GameDatabase::class.java, "pirateer_db"
        ).addCallback(dbCallback(mapConfig)).build()
        db.query("select 1", null)

    }
}