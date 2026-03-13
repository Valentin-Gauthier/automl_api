package com.example.ecodrive.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ecodrive.database.entities.Marque
import kotlinx.coroutines.flow.Flow

@Dao
interface MarqueDao {
    @Insert
    suspend fun insert(marque: Marque)

    @Query("SELECT * FROM marques")
    fun getAll(): Flow<List<Marque>>
}
