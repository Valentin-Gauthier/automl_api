package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import com.ecodrive.app.database.entities.VehicleList
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleListDao {

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    @Insert
    suspend fun insert(vehicleList: VehicleList): Long

    @Update
    suspend fun update(vehicleList: VehicleList)

    @Query("DELETE FROM Vehicle_lists WHERE id = :id")
    suspend fun deleteById(id: Int)

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM Vehicle_lists ORDER BY name ASC")
    fun getAll(): Flow<List<VehicleList>>
}
