package com.ecodrive.app.database.converter

import androidx.room.TypeConverter
import com.ecodrive.app.vehicle.GearboxType

class GearboxConverter {
    @TypeConverter
    fun toGearboxType(code: String?): GearboxType = GearboxType.from(code)

    @TypeConverter
    fun fromGearboxType(gearboxType: GearboxType): String? = gearboxType.code
}