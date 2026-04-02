package com.ecodrive.app.database.converter

import androidx.room.TypeConverter
import com.ecodrive.app.vehicle.GearboxEnergy

class EnergyConverter {
    @TypeConverter
    fun toEnergy(code: Int?): GearboxEnergy = GearboxEnergy.from(code)

    @TypeConverter
    fun fromEnergy(energy: GearboxEnergy): Int? = energy.code
}