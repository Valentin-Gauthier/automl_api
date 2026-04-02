package com.ecodrive.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Vehicles_ads",
    indices = [Index("idVehicle")],
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["idVehicle"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VehicleAd(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "idVehicle") val idVehicle: String,

    /** Chemin local ou URL de l'annonce (archive compressée et/ou posts). */
    @ColumnInfo(name = "archive_path") val archivePath: String = ""
)
