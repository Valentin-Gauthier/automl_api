package com.ecodrive.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ecodrive.app.vehicle.CarBody
import com.ecodrive.app.vehicle.GearboxEnergy
import com.ecodrive.app.vehicle.GearboxType
import com.ecodrive.app.vehicle.GenreVehicle

@Entity(
    tableName = "Vehicle_prototypes",
    indices = [
        Index("BrandId"),
        Index("vehicle_model"),
        Index("vehicle_version")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Brand::class,
            parentColumns = ["id"],
            childColumns = ["BrandId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VehiclePrototype(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // -------------------------------------------------------------------------
    // Identité du modèle
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "vehicle_model")   val name:           String,
    @ColumnInfo(name = "vehicle_version") val version:        String = "",
    @ColumnInfo(name = "collection")      val collection:     String = "",
    @ColumnInfo(name = "model_start")     val modelStartDate: String = "",
    @ColumnInfo(name = "model_end")       val modelEndDate:   String = "",

    // -------------------------------------------------------------------------
    // Classification
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "car_body")     val carBody:     CarBody      = CarBody.UNKNOW,
    @ColumnInfo(name = "vehicle_type") val vehicleType: GenreVehicle = GenreVehicle.UNKNOW,

    // -------------------------------------------------------------------------
    // Motorisation générique du modèle
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "energy")       val energy:   GearboxEnergy = GearboxEnergy.UNKNOW,
    @ColumnInfo(name = "gearbox")      val gearbox:  GearboxType   = GearboxType.UNKNOW,
    @ColumnInfo(name = "gearbox_speeds") val gearboxSpeeds:   Int  = 0,
    @ColumnInfo(name = "power_kw")       val powerKw:         Int  = 0,
    @ColumnInfo(name = "power_hp")       val powerHp:         Int  = 0,
    @ColumnInfo(name = "fuel_tank_l")    val fuelTankLiters:  Int  = 0,

    // -------------------------------------------------------------------------
    // Dimensions & capacités de série
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "door_count")      val doorCount:      Int = 0,
    @ColumnInfo(name = "passenger_count") val passengerCount: Int = 0,
    @ColumnInfo(name = "length_mm")       val lengthMm:       Int = 0,
    @ColumnInfo(name = "width_mm")        val widthMm:        Int = 0,
    @ColumnInfo(name = "height_mm")       val heightMm:       Int = 0,
    @ColumnInfo(name = "wheelbase_mm")    val wheelbaseMm:    Int = 0,

    // -------------------------------------------------------------------------
    // Clé étrangère
    // -------------------------------------------------------------------------

    @ColumnInfo(name = "BrandId") val marqueId: Int
) {
    fun isSimilarTo(other: VehiclePrototype, seuil: Int = 2): Boolean =
        (distanceTo(other)?: (seuil+1)) < seuil

    fun distanceTo(other: VehiclePrototype): Int? {
        if (id == other.id) return 0
        if (marqueId != other.marqueId) return null

        var dist = 0.0

        fun <T> enumDist(a: T, b: T, unknown: T? = null) {
            if (a == unknown || b == unknown) dist += 1
            else if (a != b ) dist += 2
        }
        fun stringDist(a: String, b: String) {
            if (a.isBlank() || b.isBlank()) dist += 1
            else if (a != b ) {
                val maxLen = maxOf(a.length, b.length)
                val lev = levenshtein(a, b)
                dist += (lev.toDouble() / maxLen) + 1
            }
        }

        enumDist(carBody,     other.carBody,     CarBody.UNKNOW)
        enumDist(vehicleType, other.vehicleType, GenreVehicle.UNKNOW)
        enumDist(energy,      other.energy,      GearboxEnergy.UNKNOW)
        enumDist(gearbox,     other.gearbox,     GearboxType.UNKNOW)

        enumDist(gearboxSpeeds,  other.gearboxSpeeds)
        enumDist(powerKw,        other.powerKw)
        enumDist(powerHp,        other.powerHp)
        enumDist(fuelTankLiters, other.fuelTankLiters)
        enumDist(doorCount,      other.doorCount)
        enumDist(passengerCount, other.passengerCount)
        enumDist(lengthMm,       other.lengthMm)
        enumDist(widthMm,        other.widthMm)
        enumDist(heightMm,       other.heightMm)
        enumDist(wheelbaseMm,    other.wheelbaseMm)

        stringDist(name,   other.name)
        stringDist(version,   other.version)
        stringDist(collection, other.collection)

        return if (dist > 0) dist.toInt() + 1 else 0
    }

    /** Distance de Levenshtein standard (Wagner-Fischer). */
    private fun levenshtein(a: String, b: String): Int {
        val m = a.length; val n = b.length
        val dp = Array(m + 1) { i -> IntArray(n + 1) { j -> if (i == 0) j else if (j == 0) i else 0 } }
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (a[i-1] == b[j-1]) dp[i-1][j-1]
                       else 1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
        }
        return dp[m][n]
    }
}
