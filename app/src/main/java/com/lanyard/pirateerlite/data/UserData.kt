package com.lanyard.pirateerlite.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lanyard.pirateerlite.singletons.User
import java.util.*

@Dao
interface UserDao {
    @Query("SELECT * FROM UserData")
    fun getUser(): LiveData<Array<UserData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserData) : Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(user: UserData)

    @Delete
    suspend fun delete(user: UserData)
}

@Entity
@TypeConverters(DateConverter::class,ArrayBoatPartConverter::class)
data class UserData (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var marketDate: Date = Date(),
    var jobDate: Date = Date(),
    var startDate: Date = Date(),
    var distance: Double = 0.0,
    var voyages: Int = 0,
    var silver: Int = 4000,
    var gold: Int = 8,
    var xp: Int = 0,
    var boatSlots: Int = 4,
    var time: Long = 0,
    var boatsSold: Int = 0,
    var parts: ArrayList<User.BoatPart> = arrayListOf(
        User.BoatPart("skiff",User.MarketItem.sails),
        User.BoatPart("skiff",User.MarketItem.rigging),
        User.BoatPart("skiff",User.MarketItem.hull))
)