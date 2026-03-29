package com.ecodrive.app.vehicle.plate.data

import android.os.Parcel
import android.os.Parcelable
import java.time.YearMonth

// =============================================================================
// Résultat principal
// =============================================================================

/**
 * Informations complètes extraites d'une plaque détectée.
 *
 * @param type                Type de plaque identifié
 * @param format              Format physique
 * @param registrationSystem  Format du numéro d'immatriculation
 * @param number              Numéro d'immatriculation
 * @param countryCode         Code du pays européen
 * @param departmentCode      Code du territoire
 * @param validityDate        Date limite de validité de la plaque
 * @param confidence          Score global de confiance (0.0 – 1.0)
 */
data class PlateInformation(
    val type: PlateType,
    val format: PlateFormat,
    val registrationSystem: RegistrationSystem, // SIV ou FNI
    val number: String,
    val countryCode: String?,           // "F", … — null si spécial ou non détecté
    val departmentCode: String?,        // "75", "2A"… — null si FNI ou non détecté
    val validityDate: YearMonth?,       // Plaque temporaire
    val confidence: Float
) : Parcelable {

    // -------------------------------------------------------------------------
    // Parcelable constructor
    // -------------------------------------------------------------------------

    private constructor(parcel: Parcel) : this(
        type = PlateType.valueOf(parcel.readString() ?: PlateType.UNKNOWN.name),
        format = PlateFormat.valueOf(parcel.readString() ?: PlateFormat.UNKNOWN.name),
        registrationSystem = RegistrationSystem.valueOf(parcel.readString() ?: RegistrationSystem.UNKNOWN.name),
        number = parcel.readString() ?: "",
        countryCode = parcel.readString(),
        departmentCode = parcel.readString(),
        validityDate = readYearMonth(parcel),
        confidence = parcel.readFloat()
    )

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type.name)
        parcel.writeString(format.name)
        parcel.writeString(registrationSystem.name)
        parcel.writeString(number)
        parcel.writeString(countryCode)
        parcel.writeString(departmentCode)
        writeYearMonth(parcel, validityDate)
        parcel.writeFloat(confidence)
    }

    override fun describeContents(): Int = 0

    // -------------------------------------------------------------------------
    // Helpers YearMonth
    // -------------------------------------------------------------------------

    private fun writeYearMonth(parcel: Parcel, value: YearMonth?) {
        if (value == null) {
            parcel.writeInt(0) // flag null
        } else {
            parcel.writeInt(1)
            parcel.writeInt(value.year)
            parcel.writeInt(value.monthValue)
        }
    }
    companion object CREATOR : Parcelable.Creator<PlateInformation> {

        private fun readYearMonth(parcel: Parcel): YearMonth? {
            val isPresent = parcel.readInt()
            return if (isPresent == 1) {
                val year = parcel.readInt()
                val month = parcel.readInt()
                YearMonth.of(year, month)
            } else {
                null
            }
        }

        // -------------------------------------------------------------------------
        // CREATOR
        // -------------------------------------------------------------------------


        override fun createFromParcel(parcel: Parcel): PlateInformation {
            return PlateInformation(parcel)
        }

        override fun newArray(size: Int): Array<PlateInformation?> {
            return arrayOfNulls(size)
        }

        fun new() = PlateInformation(
            PlateType.UNKNOWN,
            PlateFormat.UNKNOWN,
            RegistrationSystem.UNKNOWN,
            "",
            null,
            null,
            null,
            0f
        )
    }
}
