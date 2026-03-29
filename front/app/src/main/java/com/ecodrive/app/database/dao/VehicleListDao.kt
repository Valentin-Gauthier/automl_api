package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.VehicleList
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleListDao {
    @Insert
    suspend fun insert(vehicleList: VehicleList)

    @Query("SELECT * FROM Vehicle_lists")
    fun getAll(): Flow<List<VehicleList>>
}