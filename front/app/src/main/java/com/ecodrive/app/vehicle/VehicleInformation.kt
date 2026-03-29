package com.ecodrive.app.vehicle

import android.os.Parcel
import android.os.Parcelable

data class VehicleInformation(
    // =========================
    // IDENTIFICATION
    // =========================
    val vin: String,

    // =========================
    // ORIGIN
    // =========================
    val country: String,

    // =========================
    // BRAND
    // =========================
    val brandName: String,
    val brandLogoUrl: String,

    // =========================
    // MODEL
    // =========================
    val modelName: String,
    val modelVersion: String,
    val modelStartDate: String,
    val modelEndDate: String,

    val genreCode: Int?,
    val bodyCode: Int?,

    // =========================
    // POWERTRAIN
    // =========================
    val gearboxTypeCode: String?,
    val gearboxPowerLevel: Int,
    val energyCode: Int?,
    val powerKw: Int,
    val powerHp: Int,

    // =========================
    // DIMENSIONS & WEIGHT
    // =========================
    val passengerCount: Int,
    val doorCount: Int,
    val color: String,

    val weightKg: Int,
    val grossWeightKg: Int, // PTAC

    val lengthMm: Int,
    val widthMm: Int,
    val heightMm: Int,
    val wheelbaseMm: Int,

    // =========================
    // REGISTRATION & USAGE
    // =========================
    val firstRegistrationDate: String,
    val fuelTankCapacityLiters: Int,
    val collection: String,

    // =========================
    // SAISI PAR L'UTILISATEUR
    // =========================
    val mileage: Int = 75_000
) : Parcelable {
    // =========================
    // Parcelable constructor
    // =========================

    private constructor(parcel: Parcel) : this(
        vin = parcel.readString() ?: "",
        country = parcel.readString() ?: "",
        brandName = parcel.readString() ?: "",
        brandLogoUrl = parcel.readString() ?: "",
        modelName = parcel.readString() ?: "",
        modelVersion = parcel.readString() ?: "",
        modelStartDate = parcel.readString() ?: "",
        modelEndDate = parcel.readString() ?: "",
        genreCode = readNullableInt(parcel),
        bodyCode = readNullableInt(parcel),
        gearboxTypeCode = parcel.readString(),
        gearboxPowerLevel = parcel.readInt(),
        energyCode = readNullableInt(parcel),
        powerKw = parcel.readInt(),
        powerHp = parcel.readInt(),
        passengerCount = parcel.readInt(),
        doorCount = parcel.readInt(),
        color = parcel.readString() ?: "",
        weightKg = parcel.readInt(),
        grossWeightKg = parcel.readInt(),
        lengthMm = parcel.readInt(),
        widthMm = parcel.readInt(),
        heightMm = parcel.readInt(),
        wheelbaseMm = parcel.readInt(),
        firstRegistrationDate = parcel.readString() ?: "",
        fuelTankCapacityLiters = parcel.readInt(),
        collection = parcel.readString() ?: "",
        mileage = parcel.readInt()
    )

    // =========================
    // Write
    // =========================

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(vin)
        parcel.writeString(country)
        parcel.writeString(brandName)
        parcel.writeString(brandLogoUrl)
        parcel.writeString(modelName)
        parcel.writeString(modelVersion)
        parcel.writeString(modelStartDate)
        parcel.writeString(modelEndDate)

        writeNullableInt(parcel, genreCode)
        writeNullableInt(parcel, bodyCode)

        parcel.writeString(gearboxTypeCode)
        parcel.writeInt(gearboxPowerLevel)

        writeNullableInt(parcel, energyCode)

        parcel.writeInt(powerKw)
        parcel.writeInt(powerHp)

        parcel.writeInt(passengerCount)
        parcel.writeInt(doorCount)
        parcel.writeString(color)

        parcel.writeInt(weightKg)
        parcel.writeInt(grossWeightKg)

        parcel.writeInt(lengthMm)
        parcel.writeInt(widthMm)
        parcel.writeInt(heightMm)
        parcel.writeInt(wheelbaseMm)

        parcel.writeString(firstRegistrationDate)
        parcel.writeInt(fuelTankCapacityLiters)
        parcel.writeString(collection)

        parcel.writeInt(mileage)
    }

    override fun describeContents(): Int = 0

    // =========================
    // ENUM MAPPINGS
    // =========================

    val genre: GenreVehicle      get() = GenreVehicle.from(genreCode)
    val body: Carrosserie        get() = Carrosserie.from(bodyCode)
    val gearboxType: GearboxType get() = GearboxType.from(gearboxTypeCode)
    val energy: GearboxEnergy    get() = GearboxEnergy.from(energyCode)

    // =========================
    // HELPERS
    // =========================

    /** Nom affiché dans les toasts / titres */
    val displayName: String get() = "$brandName $modelName".trim()

    /** Année déduite de la date de première circulation */
    val year: Int?
        get() = when {
            firstRegistrationDate.length >= 4 && firstRegistrationDate.contains("-") ->
                firstRegistrationDate.take(4).toIntOrNull()       // "2021-03-15"
            firstRegistrationDate.length == 10 ->
                firstRegistrationDate.takeLast(4).toIntOrNull()   // "15/03/2021"
            else -> null
        }

    // =========================
    // CREATOR + HELPERS
    // =========================

    companion object CREATOR : Parcelable.Creator<VehicleInformation> {

        override fun createFromParcel(parcel: Parcel): VehicleInformation {
            return VehicleInformation(parcel)
        }

        override fun newArray(size: Int): Array<VehicleInformation?> {
            return arrayOfNulls(size)
        }

        // ---------- NULLABLE INT HELPERS ----------

        private fun writeNullableInt(parcel: Parcel, value: Int?) {
            if (value == null) {
                parcel.writeInt(0)
            } else {
                parcel.writeInt(1)
                parcel.writeInt(value)
            }
        }

        private fun readNullableInt(parcel: Parcel): Int? {
            return if (parcel.readInt() == 1) parcel.readInt() else null
        }

        // ---------- FACTORY ----------

        fun new() = VehicleInformation(
            "",
            "FR",
            "",
            "",
            "",
            "",
            "",
            "",
            null,
            null,
            null,
            0,
            null,
            0,
            0,
            0,
            0,
            "",
            0,
            0,
            0,
            0,
            0,
            0,
            "",
            0,
            ""
        )
    }
}
