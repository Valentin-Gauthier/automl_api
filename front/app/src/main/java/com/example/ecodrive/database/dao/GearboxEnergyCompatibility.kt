package com.example.ecodrive.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ecodrive.database.entities.Gearbox
import com.example.ecodrive.database.entities.GearboxEnergyCompatibility
import com.example.ecodrive.database.entities.Energy
import kotlinx.coroutines.flow.Flow

@Dao
interface GearboxEnergyCompatibilityDao {
    @Insert
    suspend fun insert(gearboxEnergyCompatibility: GearboxEnergyCompatibility)

    @Query("""
        SELECT * FROM energies
        WHERE id IN (
            SELECT energyId FROM gearbox_energy_compatibilities
            WHERE gearboxId = :gearboxId
        )
    """)
    fun getAllCompatibleEnergies(gearboxId: Int): Flow<List<Energy>>

    @Query("""
        SELECT * FROM gearbox
        WHERE id IN (
            SELECT gearboxId FROM gearbox_energy_compatibilities
            WHERE energyId = :energyId
        )
    """)
    fun getAllCompatibleGearboxes(energyId: Int): Flow<List<Gearbox>>
}