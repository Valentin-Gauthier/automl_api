package com.example.ecodrive.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "VehiclePrototype",
    foreignKeys = [
        ForeignKey(
            entity = Marque::class,
            parentColumns = ["id"],
            childColumns = ["MarqueId"]
        ),
        ForeignKey(
            entity = Energy::class,
            parentColumns = ["id"],
            childColumns = ["EnergyId"]
        ),
        ForeignKey(
            entity = Gearbox::class,
            parentColumns = ["id"],
            childColumns = ["GearboxId"]
        )
    ]
)
data class VehiclePrototype(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "vehicleModel") val name: String,
    @ColumnInfo(name = "GearboxOptions") val gearboxOptions: String,
    @ColumnInfo(name = "EnergyId") val energyId: Int,
    @ColumnInfo(name = "GearboxId") val gearboxId: Int,
    @ColumnInfo(name = "MarqueId") val marqueId: Int
)
