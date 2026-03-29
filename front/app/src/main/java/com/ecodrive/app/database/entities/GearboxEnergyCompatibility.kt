package com.ecodrive.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "Gearbox_Energies_compatibilities",
    primaryKeys = ["GearboxId", "EnergyId"],
    indices = [Index("EnergyId")],
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