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

package com.lanyard.pirateerlite.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lanyard.pirateerlite.data.*
import com.lanyard.pirateerlite.singletons.Game

/**
 * Holds the result of database requests the SplashActivity will need to setup the app.
 *
 * @author Peter Respondek
 */

class SplashViewModel : ViewModel(), Game.GameListener {
    init {
        Game.instance.addGameListener(this)
    }

    private var _dbReady = MutableLiveData<Boolean>()
    val dbReady : LiveData<Boolean> = _dbReady

    var userdata : LiveData<Array<UserData>>? = null
        private set
    var statdata : LiveData<Array<StatsData>>? = null
        private set
    var boatData : LiveData<Array<BoatData>>? = null
        private set
    var townData : LiveData<Array<TownData>>? = null
        private set
    var boatJobData : LiveData<Array<BoatJobData>>? = null
        private set
    var townJobData : LiveData<Array<TownJobData>>? = null
        private set
    var storageJobData : LiveData<Array<StorageJobData>>? = null
        private set

    /**
     * After the game singleton is setup we can start making queries to the database.
     */
    override fun onDatabaseCreated () {
        userdata = Game.instance.db.userDao().getUser()
        statdata = Game.instance.db.statsDao().getStats()
        boatData = Game.instance.db.boatDao().getBoats()
        townData = Game.instance.db.townDao().getTowns()
        _dbReady.value = true
    }

    /**
     * Get all job data for the specified town IDs
     */
    fun fetchTownJobs(town_ids: List<Long>) :  LiveData<Array<TownJobData>> {
        assert(_dbReady.value != null)
        townJobData = Game.instance.db.townJobDao().getTownsJobs(town_ids)
        return townJobData!!
    }

    /**
     * Get all job data for the specified boat IDs
     */
    fun fetchBoatJobs(boat_ids: List<Long>) : LiveData<Array<BoatJobData>> {
        assert(_dbReady.value != null)
        boatJobData = Game.instance.db.boatJobDao().getBoatsJobs(boat_ids)
        return boatJobData!!
    }

    /**
     * Get jobs in storage for the specified town IDs
     */
    fun fetchStorageJobs(town_ids: List<Long>) : LiveData<Array<StorageJobData>> {
        assert(_dbReady.value != null)
        storageJobData = Game.instance.db.storageJobDao().getStorageJobs(town_ids)
        return storageJobData!!
    }
}