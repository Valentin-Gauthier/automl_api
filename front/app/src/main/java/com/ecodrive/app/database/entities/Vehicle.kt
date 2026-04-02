package com.ecodrive.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ecodrive.app.vehicle.plate.data.PlateType
import com.ecodrive.app.vehicle.plate.data.RegistrationSystem

@Entity(
    tableName = "Vehicles",
    indices = [
        Index("PrototypeId"),
        Index("immatriculation")
    ],
    foreignKeys = [
        ForeignKey(
            entity = VehiclePrototype::class,
            parentColumns = ["id"],
            childColumns = ["PrototypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Vehicle(
    @PrimaryKey val id: String, // Peut-être le VIN, le SIV, le FNI ou autre

    // -------------------------------------------------------------------------
    // Identité légale
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "immatriculation") val immatriculation: String,
    @ColumnInfo(name = "vin") val vin: String,

    @ColumnInfo(name = "country") val country: String = "FR",
    @ColumnInfo(name = "area_code") val areaCode: String? = "",
    @ColumnInfo(name = "first_registration_date") val firstRegistrationDate: String = "",

    @ColumnInfo(name = "plate_type")   val plateType: PlateType = PlateType.UNKNOW,
    @ColumnInfo(name = "reg_system")   val regSystem: RegistrationSystem = RegistrationSystem.UNKNOW,
    @ColumnInfo(name = "validity_date") val validityDate: String? = null,

    // -------------------------------------------------------------------------
    // Spécificité technique générique
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "PrototypeId") val prototypeId: Int,

    // -------------------------------------------------------------------------
    // État physique individuel
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "color")   val color: String = "",
    @ColumnInfo(name = "mileage") val mileage: Int  = 0,

    // -------------------------------------------------------------------------
    // Valeur marchande estimée (remplie par PredictActivity)
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "price_new_min")  val priceNewMin:  Int = 0,
    @ColumnInfo(name = "price_new_max")  val priceNewMax:  Int = 0,
    @ColumnInfo(name = "price_used_min") val priceUsedMin: Int = 0,
    @ColumnInfo(name = "price_used_max") val priceUsedMax: Int = 0,

    // -------------------------------------------------------------------------
    // Médias
    // -------------------------------------------------------------------------

    /** URL ou chemin local de la photo principale de CE véhicule. */
    @ColumnInfo(name = "photo_main") val photoMain: String = ""
) {
    fun isSimilarTo(other: Vehicle, protoDistance: Int, seuil: Int = 2): Boolean =
        (distanceTo(other, protoDistance) ?: (seuil + 1)) < seuil

    /**
     * Distance métrique entre deux véhicules, tenant compte de leurs prototypes.
     *
     * | Cas                             | Distance                           |
     * |---------------------------------|------------------------------------|
     * | Même id                         | 0                                  |
     * | Même VIN (non vide)             | 0                                  |
     * | VIN vide + même immatriculation | dist (dates) × dist (proto)          |
     * | VIN vide + immatriculation vide | (dist (dates)/5 + dist (proto)) × 10 |
     * | Tout autre cas                  | null (infini)                      |
     *
     * [protoDistance] doit être fourni par l'appelant (issu de [VehiclePrototype.distanceTo])
     * afin d'éviter de charger les prototypes dans cette méthode.
     */
    fun Vehicle.distanceTo(other: Vehicle, protoDistance: Int): Int? {
        if (id == other.id) return 0
        if (vin.isNotBlank() && other.vin.isNotBlank())
            return if( vin == other.vin) 0 else null

        val dateDist = dateDistance(firstRegistrationDate, other.firstRegistrationDate)
        var dist: Int? = null

        if (immatriculation.isNotBlank() && other.immatriculation.isNotBlank()) {
            if (immatriculation == other.immatriculation)
                dist = (dateDist + protoDistance)
        } else {
            dist = (dateDist / 5 + protoDistance) * 10
        }

        return dist
   }

    /** Distance entre deux dates de mise en service (format "YYYY-MM-DD" ou "YYYY"). */
    private fun dateDistance(a: String, b: String): Int {
        if (a.isBlank() || b.isBlank()) return 0
        val ya = a.take(4).toIntOrNull() ?: return 0
        val yb = b.take(4).toIntOrNull() ?: return 0
        return kotlin.math.abs(ya - yb)
    }
}
