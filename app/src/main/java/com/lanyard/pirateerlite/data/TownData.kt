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
import com.lanyard.pirateerlite.models.TownModel
import java.io.Serializable
import java.util.*

@Dao
interface TownDao {
    @Query("SELECT * FROM TownData")
    fun getTowns(): LiveData<Array<TownData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(town: TownData)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(town: TownData)

    @Delete
    suspend fun delete(town: TownData)
}

@Entity
@TypeConverters(DateConverter::class, TownTypeConverter::class, AllegianceConverter::class, HarbourSizeConverter::class)
data class TownData (
    @PrimaryKey
    var id: Long,
    var name : String,
    var type : TownModel.TownType,
    var allegiance : TownModel.Allegiance,
    var harbour : TownModel.HarbourSize,
    var description : String,
    var level : Int = 0,
    var jobsTimeStamp : Date = Date(),
    var totalVisits : Int = 0,
    var startSilver : Int = 0,
    var endSilver : Int = 0,
    var startGold : Int = 0,
    var endGold : Int = 0)