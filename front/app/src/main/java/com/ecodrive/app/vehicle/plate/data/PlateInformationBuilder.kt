package com.ecodrive.app.vehicle.plate.data

import android.graphics.Rect
import com.ecodrive.app.vehicle.plate.data.section.EuSection
import com.ecodrive.app.vehicle.plate.data.section.MainSection
import com.ecodrive.app.vehicle.plate.data.section.RegionalSection
import com.ecodrive.app.vehicle.plate.data.section.ValiditySection
import com.ecodrive.app.vehicle.plate.extraction.TextBlock
import java.time.YearMonth

// =============================================================================
// Résultat principal
// =============================================================================

/**
 * Informations complètes extraites d'une plaque détectée.
 *
 * @param type                Type de plaque identifié
 * @param format              Format physique
 * @param plateArea           Rectangle de la plaque dans l'image originale
 * @param mainSection         Section centrale — toujours présente si la plaque est valide
 * @param euSection           Bandeau EU (peut être null si absent)
 * @param regionalSection     Bandeau régional (null si FNI ou collection ancienne)
 * @param validitySection     Section date (uniquement pour WW / temporaire)
 * @param confidence          Score global de confiance (0.0 – 1.0)
 * @param rawBlocks           Tous les TextBlock ayant contribué à la détection
 */
data class PlateInformationBuilder(
    private val type: PlateType,
    private val format: PlateFormat,
    private val plateArea: Rect,
    private val mainSection: MainSection?,
    private val euSection: EuSection?,
    private val regionalSection: RegionalSection?,
    private val validitySection: ValiditySection?,
    private val confidence: Float,
    private val rawBlocks: List<TextBlock>
) {
    /** Numéro d'immatriculation normalisé, ou null si non trouvé */
    val registrationNumber: Pair<RegistrationSystem, String>?
        get() = mainSection?.let { Pair(it.system, it.normalizedNumber) }

    /** Numéro de département, ou null si non disponible */
    val departmentCode: String?
        get() = regionalSection?.departmentCode

    /** Numéro de département, ou null si non disponible */
    val validityDate: YearMonth?
        get() = validitySection?.let {
            if (type == PlateType.TEMPORARY)
                YearMonth.of(it.year, it.month)
            else
                null
        }

    /** Indique si une plaque valide a été trouvée */
    val isValid: Boolean
        get() = type != PlateType.UNKNOWN && mainSection != null && confidence >= MIN_CONFIDENCE

    fun build() : PlateInformation {
        var plate: PlateInformation = PlateInformation.new()
        if (isValid)
            mainSection?.let {
                val (system, number) = registrationNumber ?: Pair(RegistrationSystem.UNKNOWN, "")
                plate = PlateInformation(
                    type,
                    format,
                    system,  // SIV ou FNI
                    number,
                    euSection?.countryCode,           // "F", … — null si spécial ou non détecté
                    departmentCode,                   // "75", "2A"… — null si FNI ou non détecté
                    validityDate,                     // Plaque temporaire
                    confidence
                )
            }
        return plate
    }

    companion object {
        const val MIN_CONFIDENCE = 0.50f
    }
}
