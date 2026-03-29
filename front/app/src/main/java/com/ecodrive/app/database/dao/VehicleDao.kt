package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ecodrive.app.database.entities.Brand
import com.ecodrive.app.database.entities.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: Vehicle)

    @Query("DELETE FROM Vehicles WHERE immatriculation = :immatriculation")
    suspend fun deleteById(immatriculation: String)

    @Query("SELECT * FROM Vehicles WHERE immatriculation = :immatriculation LIMIT 1")
    suspend fun getById(immatriculation: String): Vehicle?

    @Query("SELECT * FROM Vehicles")
    fun getAll(): Flow<List<Vehicle>>

    @Query("""
        SELECT * FROM Brands
        WHERE id = (
            SELECT BrandId FROM Vehicle_prototypes
            WHERE id=:vehiclePrototypeId
        )
        LIMIT 1
    """)
    fun getBrandOf(vehiclePrototypeId: Int): Flow<Brand>
}
