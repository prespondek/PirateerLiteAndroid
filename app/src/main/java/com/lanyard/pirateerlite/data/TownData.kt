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
    var id: Int,
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