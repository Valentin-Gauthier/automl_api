package com.example.ecodrive.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "Vehicles_in_lists",
    primaryKeys = ["immatriculation", "ListId"],
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["immatriculation"],
            childColumns = ["immatriculation"]
        ),
        ForeignKey(
            entity = VehicleList::class,
            parentColumns = ["id"],
            childColumns = ["ListId"]
        )
    ]
)
data class VehicleInList(
    @ColumnInfo(name = "immatriculation") val immatriculation: String,
    @ColumnInfo(name = "ListId") val listId: Int
)
