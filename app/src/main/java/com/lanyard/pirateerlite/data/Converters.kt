package com.lanyard.pirateerlite.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import java.util.*
import com.google.gson.reflect.TypeToken
import com.lanyard.pirateerlite.models.JobModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.User


object DateConverter {
    @TypeConverter
    @JvmStatic
    fun toDate(timestamp: Long?): Date? {
        return Date(timestamp!!)
    }

    @TypeConverter
    @JvmStatic
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}

object TownTypeConverter {
    @TypeConverter
    @JvmStatic
    fun toTownType(str: String?): TownModel.TownType? {
        return TownModel.TownType.valueOf(str!!)
    }

    @TypeConverter
    @JvmStatic
    fun fromTownType(type: TownModel.TownType?): String? {
        return type?.name
    }
}

object HarbourSizeConverter {
    @TypeConverter
    @JvmStatic
    fun toHarbourSize(str: String?): TownModel.HarbourSize? {
        return TownModel.HarbourSize.valueOf(str!!)
    }

    @TypeConverter
    @JvmStatic
    fun fromHarbourSize(type: TownModel.HarbourSize?): String? {
        return type?.name
    }
}

object AllegianceConverter {
    @TypeConverter
    @JvmStatic
    fun toAllegiance(str: String?): TownModel.Allegiance? {
        return TownModel.Allegiance.valueOf(str!!)
    }

    @TypeConverter
    @JvmStatic
    fun fromAllegiance(type: TownModel.Allegiance?): String? {
        return type?.name
    }
}


object ArrayIntConverter {
    @TypeConverter
    @JvmStatic
    fun toArray(str: String): Array<Int>? {
        val listType = object : TypeToken<Array<Int>>() {}.type
        return Gson().fromJson(str, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromArray(arr: Array<Int>?): String? {
        return Gson().toJson(arr)
    }
}

object ArrayBoatPartConverter {
    @TypeConverter
    @JvmStatic
    fun toArrayList(str: String): ArrayList<User.BoatPart>? {
        val listType = object : TypeToken<ArrayList<User.BoatPart>>() {}.type
        return Gson().fromJson(str, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromArrayList(arr: ArrayList<User.BoatPart>?): String? {
        return Gson().toJson(arr)
    }
}