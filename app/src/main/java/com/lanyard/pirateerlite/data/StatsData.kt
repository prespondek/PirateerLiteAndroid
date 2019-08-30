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
