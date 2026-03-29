package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.Brand
import com.ecodrive.app.database.entities.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Insert
    suspend fun insert(vehicle: Vehicle)

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