package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.Gearbox
import kotlinx.coroutines.flow.Flow

@Dao
interface GearboxDao {
    @Insert
    suspend fun insert(gearbox: Gearbox)

    @Query("SELECT * FROM Gearbox")
    fun getAll(): Flow<List<Gearbox>>
}