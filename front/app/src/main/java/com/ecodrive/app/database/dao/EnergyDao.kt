package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.Energy
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyDao {
    @Insert
    suspend fun insert(energy: Energy)

    @Query("SELECT * FROM Energies")
    fun getAll(): Flow<List<Energy>>
}