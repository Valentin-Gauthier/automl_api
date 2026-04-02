package com.ecodrive.app.database.converter

import androidx.room.TypeConverter
import com.ecodrive.app.vehicle.plate.data.RegistrationSystem

class RegistrationSystemConverter {
    @TypeConverter
    fun toRegistrationSystem(name: String?): RegistrationSystem =
        name?.let { runCatching { RegistrationSystem.valueOf(it) }.getOrNull() } ?: RegistrationSystem.UNKNOW

    @TypeConverter
    fun fromRegistrationSystem(system: RegistrationSystem): String = system.name
}