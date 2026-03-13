package com.example.ecodrive.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "gearbox_energy_compatibilities",
    primaryKeys = ["GearboxId", "EnergyId"],
    foreignKeys = [
        ForeignKey(
            entity = Gearbox::class,
            parentColumns = ["id"],
            childColumns = ["GearboxId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Energy::class,
            parentColumns = ["id"],
            childColumns = ["EnergyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GearboxEnergyCompatibility(
    @ColumnInfo(name = "GearboxId") val gearboxId: Int,
    @ColumnInfo(name = "EnergyId") val energyId: Int
)
