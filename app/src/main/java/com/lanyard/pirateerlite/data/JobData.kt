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
interface StorageJobDao {
    @Query("SELECT * FROM StorageJobData WHERE townid LIKE :townid")
    fun getJobs(townid: Long): LiveData<Array<StorageJobData>>

    @Query("SELECT * FROM StorageJobData WHERE townid IN (:townid) ORDER BY townid")
    fun getStorageJobs(townid: List<Long>): LiveData<Array<StorageJobData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(storageData: List<StorageJobData>) : List<Long>

    @Delete
    suspend fun delete(storageData: List<StorageJobData>)

    @Query("DELETE FROM StorageJobData WHERE townid = :townid")
    suspend fun deleteByTownId(townid: Long)
}

@Dao
interface TownJobDao {
    @Query("SELECT * FROM TownJobData WHERE townid LIKE :townid")
    fun getTownJobs(townid: Long): LiveData<Array<TownJobData>>

    @Query("SELECT * FROM TownJobData WHERE townid IN (:townid) ORDER BY townid")
    fun getTownsJobs(townid: List<Long>): LiveData<Array<TownJobData>>

    @Query("SELECT * FROM TownJobData")
    fun getAllTownJobs(): LiveData<Array<TownJobData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(townData: List<TownJobData>) : List<Long>

    @Delete
    suspend fun delete(townData: List<TownJobData>)

    @Query("DELETE FROM TownJobData WHERE townid = :townid")
    suspend fun deleteByTownId(townid: Long)
}

@Dao
interface BoatJobDao {
    @Query("SELECT * FROM BoatJobData WHERE boatid LIKE :boatid")
    fun getBoatJobs(boatid: Long): LiveData<Array<BoatJobData>>

    @Query("SELECT * FROM BoatJobData WHERE boatid IN (:boatid) ORDER BY boatid")
    fun getBoatsJobs(boatid: List<Long>): LiveData<Array<BoatJobData>>

    @Query("SELECT * FROM BoatJobData")
    fun getAllBoatsJobs(): LiveData<Array<BoatJobData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(boatData: List<BoatJobData>) : List<Long>

    @Delete
    suspend fun delete(boatData: List<BoatJobData>)

    @Query("DELETE FROM BoatJobData WHERE boatid = :boatid")
    suspend fun deleteByBoatId(boatid: Long)
}


@Entity(foreignKeys = arrayOf(
    ForeignKey(
        onDelete = CASCADE,
        entity = TownData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("sourceTownId")),
    ForeignKey(
        entity = TownData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("destinationTownId"))))
data class JobData (
    var type:               String,
    var value:              Float,
    var gold:               Boolean,
    var sourceTownId:       Long,
    var destinationTownId:  Long,
    var multiplier:         Double)

@Entity(foreignKeys = arrayOf(
    ForeignKey(
        onDelete = CASCADE,
        entity = TownData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("townid"))))
@TypeConverters(DateConverter::class)
data class TownJobData (
    @PrimaryKey(autoGenerate = true)
    var id:                 Long,
    @ColumnInfo(index = true)
    val townid:             Long,
    val dateCreated:        Date,
    @Embedded val jobData: JobData)

@Entity(foreignKeys = arrayOf(
    ForeignKey(
        onDelete = CASCADE,
        entity = TownData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("townid"))))
data class StorageJobData (
    @PrimaryKey(autoGenerate = true)
    var id:                 Long,
    @ColumnInfo(index = true)
    val townid:             Long,
    @Embedded val jobData: JobData)

@Entity(foreignKeys = arrayOf(
    ForeignKey(
        onDelete = CASCADE,
        entity = BoatData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("boatid"))))
data class BoatJobData (
    @PrimaryKey(autoGenerate = true)
    var id:                 Long,
    @ColumnInfo(index = true)
    val boatid:             Long,
    @Embedded val jobData: JobData)
