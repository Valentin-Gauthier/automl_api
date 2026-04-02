package com.ecodrive.app.database.converter

import androidx.room.TypeConverter
import com.ecodrive.app.vehicle.plate.data.PlateType

class PlateTypeConverter {
    @TypeConverter
    fun toPlateType(name: String?): PlateType =
        name?.let { runCatching { PlateType.valueOf(it) }.getOrNull() } ?: PlateType.UNKNOW

    @TypeConverter
    fun fromPlateType(plateType: PlateType): String = plateType.name
}