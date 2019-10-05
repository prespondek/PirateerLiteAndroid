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


object ArrayLongConverter {
    @TypeConverter
    @JvmStatic
    fun toArray(str: String): Array<Long>? {
        val listType = object : TypeToken<Array<Long>>() {}.type
        return Gson().fromJson(str, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromArray(arr: Array<Long>?): String? {
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