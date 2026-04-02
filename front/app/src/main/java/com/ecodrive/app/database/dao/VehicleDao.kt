package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ecodrive.app.database.dto.VehicleGalleryItem
import com.ecodrive.app.database.dto.VehicleWithDetails
import com.ecodrive.app.database.entities.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: Vehicle)

    @Query("DELETE FROM Vehicles WHERE id = :id")
    suspend fun deleteById(id: String)

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM Vehicles")
    suspend fun getAll(): List<Vehicle>

    @Query("SELECT * FROM Vehicles")
    fun getAllGalleryItems(listId: Int): Flow<List<VehicleGalleryItem>>

    @Query("""
        SELECT * FROM Vehicles
        WHERE immatriculation IN (
            SELECT immatriculation FROM Vehicles_in_Lists
            WHERE ListId = :listId
        )
    """)
    fun getGalleryItemsForList(listId: Int): Flow<List<VehicleGalleryItem>>

    @Query("""
        SELECT * FROM Vehicles
        WHERE immatriculation NOT IN (
            SELECT immatriculation FROM Vehicles_in_Lists
        )
    """)
    fun getGalleryItemsWithoutList(): Flow<List<VehicleGalleryItem>>

    @Transaction
    @Query("SELECT * FROM Vehicles WHERE id = :id LIMIT 1")
    suspend fun getWithDetails(id: String): VehicleWithDetails?

    @Query("SELECT * FROM Vehicles WHERE PrototypeId = :prototypeId")
    suspend fun getSameModels(prototypeId: Int): List<Vehicle>

    suspend fun getSameModels(vehicle: Vehicle): List<Vehicle> =
        getSameModels(vehicle.prototypeId)
}
