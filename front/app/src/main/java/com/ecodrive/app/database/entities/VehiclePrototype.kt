package com.ecodrive.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Vehicle_prototypes",
    indices = [
        Index("BrandId"),
        Index("EnergyId"),
        Index("GearboxId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Brand::class,
            parentColumns = ["id"],
            childColumns = ["BrandId"]
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
    @ColumnInfo(name = "vehicle_Model") val name: String,
    @ColumnInfo(name = "gearbox_options") val gearboxOptions: String,
    @ColumnInfo(name = "EnergyId") val energyId: Int,
    @ColumnInfo(name = "GearboxId") val gearboxId: Int,
    @ColumnInfo(name = "BrandId") val marqueId: Int
)
