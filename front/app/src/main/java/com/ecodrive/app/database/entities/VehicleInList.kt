package com.ecodrive.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "Vehicles_in_Lists",
    primaryKeys = ["VehicleId", "ListId"],
    indices = [Index("ListId")],
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["VehicleId"]
        ),
        ForeignKey(
            entity = VehicleList::class,
            parentColumns = ["id"],
            childColumns = ["ListId"]
        )
    ]
)
data class VehicleInList(
    @ColumnInfo(name = "VehicleId") val vehicleId: String,
    @ColumnInfo(name = "ListId") val listId: Int
)
