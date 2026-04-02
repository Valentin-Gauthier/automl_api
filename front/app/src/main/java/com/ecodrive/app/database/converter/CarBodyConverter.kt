package com.ecodrive.app.database.converter

import androidx.room.TypeConverter
import com.ecodrive.app.vehicle.CarBody

class CarBodyConverter {
    @TypeConverter
    fun toCarBody(code: Int?): CarBody = CarBody.from(code)

    @TypeConverter
    fun fromCarBody(carBody: CarBody): Int? = carBody.code
}