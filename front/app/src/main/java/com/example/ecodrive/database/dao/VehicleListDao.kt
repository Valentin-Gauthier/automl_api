package com.example.ecodrive.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ecodrive.database.entities.VehicleList
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleListDao {
    @Insert
    suspend fun insert(vehicleList: VehicleList)

    @Query("SELECT * FROM VehicleLists")
    fun getAll(): Flow<List<VehicleList>>
}