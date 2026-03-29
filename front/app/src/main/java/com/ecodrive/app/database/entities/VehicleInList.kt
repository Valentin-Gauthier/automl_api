package com.ecodrive.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "Vehicles_in_Lists",
    primaryKeys = ["immatriculation", "ListId"],
    indices = [Index("ListId")],
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
