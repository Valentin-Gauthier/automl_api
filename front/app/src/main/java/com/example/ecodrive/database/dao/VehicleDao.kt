package com.example.ecodrive.database.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.ecodrive.database.entities.Vehicle

@Dao
interface VehicleDao {
    @Insert
    suspend fun insert(vehicle: Vehicle)
}
