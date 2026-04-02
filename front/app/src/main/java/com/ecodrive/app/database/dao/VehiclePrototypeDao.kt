package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ecodrive.app.database.entities.VehiclePrototype
import kotlinx.coroutines.flow.Flow

@Dao
abstract class VehiclePrototypeDao {

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    @Insert
    abstract suspend fun insert(prototype: VehiclePrototype): Long

    @Update
    abstract suspend fun update(prototype: VehiclePrototype)

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM Vehicle_prototypes")
    abstract fun getAll(): Flow<List<VehiclePrototype>>

    @Query("""
        SELECT * FROM Vehicle_prototypes
        WHERE id = :id
           OR (vehicle_model = :name AND BrandId = :brandId)
    """)
    abstract suspend fun getAlmostSimilar(id: Int, name: String, brandId: Int): List<VehiclePrototype>

    suspend fun getAlmostSimilar(v: VehiclePrototype): List<VehiclePrototype> =
        getAlmostSimilar(v.id, v.name, v.marqueId)

    suspend fun getSimilar(v: VehiclePrototype): List<VehiclePrototype> =
        getAlmostSimilar(v).filter { v.isSimilarTo(it) }

    // -------------------------------------------------------------------------
    // Suppression
    // -------------------------------------------------------------------------

    @Query("DELETE FROM Vehicle_prototypes WHERE id = :id")
    abstract suspend fun deleteById(id: Int)

    @Query("""
        DELETE FROM Vehicle_prototypes
        WHERE id NOT IN (SELECT DISTINCT PrototypeId FROM Vehicles)
    """)
    abstract suspend fun deleteOrphans()
}
