package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.VehiclePrototype
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiclePrototypeDao {
    @Insert
    suspend fun insert(vehiclePrototype: VehiclePrototype)

    @Query("SELECT * FROM Vehicle_prototypes")
    fun getAll(): Flow<List<VehiclePrototype>>
}