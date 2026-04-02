package com.ecodrive.app.database.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.ecodrive.app.database.entities.Vehicle
import com.ecodrive.app.database.entities.VehicleAd
import com.ecodrive.app.database.entities.VehiclePrototype
import com.ecodrive.app.database.entities.Brand

/**
 * Véhicule complet avec prototype, marque et annonces associées.
 * Utilisé par MoreActivity pour l'affichage détaillé.
 */
data class VehicleWithDetails(
    @Embedded val vehicle: Vehicle,
    @Relation(
        parentColumn = "PrototypeId",
        entityColumn = "id",
        entity = VehiclePrototype::class
    )
    val prototypeWithBrand: PrototypeWithBrand,
    @Relation(
        parentColumn = "id",
        entityColumn = "idVehicle"
    )
    val ads: List<VehicleAd>
)