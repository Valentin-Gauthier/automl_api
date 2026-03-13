package com.example.ecodrive.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ecodrive.database.entities.Vehicle
import com.example.ecodrive.database.entities.VehicleInList
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleInListDao {
    @Insert
    suspend fun insert(vehicleInList: VehicleInList)

    @Query("""
        SELECT * FROM Vehicles
        WHERE immatriculation IN (
            SELECT immatriculation FROM Vehicle_in_list
            WHERE ListId=:list 
        )
    """)
    fun getAll(list: Int): Flow<List<Vehicle>>
}
