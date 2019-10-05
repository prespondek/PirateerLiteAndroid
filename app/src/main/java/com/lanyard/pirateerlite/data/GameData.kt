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

package com.lanyard.pirateerlite.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = arrayOf(
        UserData::class,
        BoatData::class,
        TownData::class,
        StatsData::class,
        TownJobData::class,
        BoatJobData::class,
        StorageJobData::class),
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun boatDao(): BoatDao
    abstract fun townDao(): TownDao
    abstract fun townJobDao(): TownJobDao
    abstract fun boatJobDao(): BoatJobDao
    abstract fun storageJobDao(): StorageJobDao
    abstract fun statsDao(): StatsDao
}