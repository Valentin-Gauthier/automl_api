package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ecodrive.app.database.entities.VehicleAd
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleAdDao {

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    @Insert
    suspend fun insert(vehicleAd: VehicleAd): Long

    @Update
    suspend fun update(vehicleAd: VehicleAd)

    @Query("DELETE FROM Vehicles_ads WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM Vehicles_ads WHERE idVehicle = :vehicleId")
    fun getAllAdsFrom(vehicleId: String): Flow<List<VehicleAd>>
}
