package com.lanyard.pirateerlite.data

import androidx.lifecycle.LiveData
import androidx.room.*
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
@TypeConverters(DateConverter::class,ArrayIntConverter::class)
data class BoatData (
    @PrimaryKey(autoGenerate = true)
    var id:             Long,
    var type:           String,
    var name:           String,
    var townid:         Int,
    var cargoSize:      Int,
    var totalDistance:  Double = 0.0,
    var totalVoyages:   Int = 0,
    var totalSilver:    Int = 0,
    var totalGold:      Int = 0,
    var datePurchased:  Date = Date(),
    var departureTime:  Long = 0,
    var courseTime:     Long = 0,
    var course:         Array<Int> = arrayOf()
)
