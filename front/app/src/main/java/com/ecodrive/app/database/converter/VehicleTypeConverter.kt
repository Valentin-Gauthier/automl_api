package com.ecodrive.app.database.converter

import androidx.room.TypeConverter
import com.ecodrive.app.vehicle.GenreVehicle

class VehicleTypeConverter {
    @TypeConverter
    fun toGenreVehicle(code: Int?): GenreVehicle = GenreVehicle.from(code)

    @TypeConverter
    fun fromGenreVehicle(genreVehicle: GenreVehicle): Int? = genreVehicle.code
}