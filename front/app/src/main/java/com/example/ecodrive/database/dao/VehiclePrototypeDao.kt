package com.example.ecodrive.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ecodrive.database.entities.VehiclePrototype
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiclePrototypeDao {
    @Insert
    suspend fun insert(vehiclePrototype: VehiclePrototype)

    @Query("SELECT * FROM VehiclePrototype")
    fun getAll(): Flow<List<VehiclePrototype>>
}
