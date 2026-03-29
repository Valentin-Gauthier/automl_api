package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.Vehicle
import com.ecodrive.app.database.entities.VehicleInList
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleInListDao {
    @Insert
    suspend fun insert(vehicleInList: VehicleInList): Long

    @Query("""
        DELETE FROM Vehicles_in_Lists
        WHERE immatriculation = :immatriculation
            AND ListId = :listId
    """)
    suspend fun deleteById(immatriculation: String, listId: Int)

    @Query("""
        SELECT * FROM Vehicles
        WHERE immatriculation IN (
            SELECT immatriculation FROM Vehicles_in_Lists
            WHERE ListId=:list
        )
    """)
    fun getAllFrom(list: Int): Flow<List<Vehicle>>
}
