package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.Brand
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandDao {
    @Insert
    suspend fun insert(brand: Brand)

    @Query("SELECT * FROM Brands")
    fun getAll(): Flow<List<Brand>>
}