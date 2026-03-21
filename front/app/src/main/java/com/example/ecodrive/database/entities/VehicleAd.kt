package com.example.ecodrive.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Vehicles_ads",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["immatriculation"],
            childColumns = ["immatriculation"]
        )
    ]
)
data class VehicleAd(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "immatriculation") val immatriculation: String,
    @ColumnInfo(name = "archive") val pp: String
)
