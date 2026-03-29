package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.VehicleAd
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleAdDao {
    @Insert
    suspend fun insert(vehicleAd: VehicleAd)

    @Query("SELECT * FROM Vehicles_ads WHERE immatriculation=:vehicle")
    fun getAllAdsFrom(vehicle: String): Flow<List<VehicleAd>>
}