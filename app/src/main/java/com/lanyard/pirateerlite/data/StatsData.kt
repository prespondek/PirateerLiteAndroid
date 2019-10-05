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

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import java.util.*

@Dao
interface StatsDao {
    @Query("SELECT * FROM StatsData")
    fun getStats(): LiveData<Array<StatsData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stat: StatsData)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(stat: StatsData)

    @Delete
    suspend fun delete(stat: StatsData)
}

@Entity
data class StatsData(
    @PrimaryKey
    var stat: String,
    @Embedded var boatData: BoatData)
