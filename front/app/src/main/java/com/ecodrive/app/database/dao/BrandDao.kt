package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ecodrive.app.database.entities.Brand

@Dao
interface BrandDao {

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    @Insert suspend fun insert(brand: Brand): Long

    @Update suspend fun update(brand: Brand)

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM Brands ORDER BY name ASC")
    suspend fun getAll(): List<Brand>

    @Query("SELECT * FROM Brands WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Brand?

    // -------------------------------------------------------------------------
    // Suppression
    // -------------------------------------------------------------------------

    @Query("DELETE FROM Brands WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("""
        DELETE FROM Brands
        WHERE id NOT IN (SELECT DISTINCT BrandId FROM Vehicle_prototypes)
    """)
    suspend fun deleteOrphans()
}
