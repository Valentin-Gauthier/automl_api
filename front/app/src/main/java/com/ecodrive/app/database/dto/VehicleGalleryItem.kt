package com.ecodrive.app.database.dto

import androidx.room.ColumnInfo

/**
 * Projection légère utilisée par GalleryManager.
 * Contient uniquement les champs nécessaires à l'affichage d'une vignette.
 */
data class VehicleGalleryItem(
    @ColumnInfo(name = "id")             val id: String,
    @ColumnInfo(name = "immatriculation") val immatriculation: String,
    @ColumnInfo(name = "price_used_min") val priceUsedMin: Int,
    @ColumnInfo(name = "price_used_max") val priceUsedMax: Int,
    @ColumnInfo(name = "PrototypeId")    val prototypeId: Int,
    @ColumnInfo(name = "photo_main")     val photoMain: String
)