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