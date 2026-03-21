package com.example.ecodrive.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Vehicles",
    foreignKeys = [
        ForeignKey(
            entity = VehiclePrototype::class,
            parentColumns = ["id"],
            childColumns = ["PrototypeId"]
        )
    ]
)
data class Vehicle(
    @PrimaryKey val immatriculation: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "couleur") val couleur: Int,
    @ColumnInfo(name = "mileage") val mileage: Int,
    @ColumnInfo(name = "price_new_min") val priceNewMin: Int,
    @ColumnInfo(name = "price_new_max") val priceNewMax: Int,
    @ColumnInfo(name = "price_used_min") val priceUsedMin: Int,
    @ColumnInfo(name = "price_used_max") val priceUsedMax: Int,
    @ColumnInfo(name = "pp") val pp: String,
    @ColumnInfo(name = "PrototypeId") val prototypeId: Int
)
