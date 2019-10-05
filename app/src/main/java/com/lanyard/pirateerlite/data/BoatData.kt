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
interface BoatDao {
    @Query("SELECT * FROM BoatData")
    fun getBoats(): LiveData<Array<BoatData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(boatData: BoatData) : Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(boatData: BoatData)

    @Delete
    suspend fun delete(boatData: BoatData)
}

@Entity
@TypeConverters(DateConverter::class,ArrayLongConverter::class)
data class BoatData (
    @PrimaryKey(autoGenerate = true)
    var id:             Long,
    var type:           String,
    var name:           String,
    var townid:         Long,
    var cargoSize:      Int,
    var totalDistance:  Double = 0.0,
    var totalVoyages:   Int = 0,
    var totalSilver:    Int = 0,
    var totalGold:      Int = 0,
    var datePurchased:  Date = Date(),
    var departureTime:  Long = 0,
    var courseTime:     Long = 0,
    var course:         Array<Long> = arrayOf()
)
