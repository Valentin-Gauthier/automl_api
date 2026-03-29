package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.Gearbox
import com.ecodrive.app.database.entities.GearboxEnergyCompatibility
import com.ecodrive.app.database.entities.Energy
import kotlinx.coroutines.flow.Flow

@Dao
interface GearboxEnergyCompatibilityDao {
    @Insert
    suspend fun insert(gearboxEnergyCompatibility: GearboxEnergyCompatibility)

    @Query("""
        SELECT * FROM Energies
        WHERE id IN (
            SELECT energyId FROM Gearbox_Energies_compatibilities
            WHERE gearboxId = :gearboxId
        )
    """)
    fun getAllCompatibleEnergies(gearboxId: Int): Flow<List<Energy>>

    @Query("""
        SELECT * FROM Gearbox
        WHERE id IN (
            SELECT gearboxId FROM Gearbox_Energies_compatibilities
            WHERE energyId = :energyId
        )
    """)
    fun getAllCompatibleGearboxes(energyId: Int): Flow<List<Gearbox>>
}