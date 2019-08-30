package com.lanyard.pirateerlite.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Dao
interface StorageJobDao {
    @Query("SELECT * FROM StorageJobData WHERE townid LIKE :townid")
    fun getJobs(townid: Int): LiveData<Array<StorageJobData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(storageData: List<StorageJobData>) : List<Long>

    @Delete
    suspend fun delete(storageData: List<StorageJobData>)

    @Query("DELETE FROM StorageJobData WHERE townid = :townid")
    suspend fun deleteByTownId(townid: Int)
}

@Dao
interface TownJobDao {
    @Query("SELECT * FROM TownJobData WHERE townid LIKE :townid")
    fun getJobs(townid: Int): LiveData<Array<TownJobData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(townData: List<TownJobData>) : List<Long>

    @Delete
    suspend fun delete(townData: List<TownJobData>)

    @Query("DELETE FROM TownJobData WHERE townid = :townid")
    suspend fun deleteByTownId(townid: Int)
}

@Dao
interface BoatJobDao {
    @Query("SELECT * FROM BoatJobData WHERE boatid LIKE :boatid")
    fun getJobs(boatid: Long): LiveData<Array<BoatJobData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(boatData: List<BoatJobData>) : List<Long>

    @Delete
    suspend fun delete(boatData: List<BoatJobData>)

    @Query("DELETE FROM BoatJobData WHERE boatid = :boatid")
    suspend fun deleteByBoatId(boatid: Long)
}


@Entity(foreignKeys = arrayOf(
    ForeignKey(
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
    var sourceTownId:       Int,
    var destinationTownId:  Int,
    var multiplier:         Double)

@Entity(foreignKeys = arrayOf(
    ForeignKey(
        entity = TownData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("townid"))))
@TypeConverters(DateConverter::class)
data class TownJobData (
    @PrimaryKey(autoGenerate = true)
    var id:                 Long,
    @ColumnInfo(index = true)
    val townid:             Int,
    val dateCreated:        Date,
    @Embedded val jobData: JobData)

@Entity(foreignKeys = arrayOf(
    ForeignKey(
        entity = TownData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("townid"))))
data class StorageJobData (
    @PrimaryKey(autoGenerate = true)
    var id:                 Long,
    @ColumnInfo(index = true)
    val townid:             Int,
    @Embedded val jobData: JobData)

@Entity(foreignKeys = arrayOf(
    ForeignKey(
        entity = BoatData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("boatid"))))
data class BoatJobData (
    @PrimaryKey(autoGenerate = true)
    var id:                 Long,
    @ColumnInfo(index = true)
    val boatid:             Long,
    @Embedded val jobData: JobData)
