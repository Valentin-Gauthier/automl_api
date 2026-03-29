package com.ecodrive.app.vehicle.plate.data.section

import com.ecodrive.app.vehicle.plate.data.RegistrationSystem
import com.ecodrive.app.vehicle.plate.extraction.TextBlock

/**
 * Section centrale : numéro d'immatriculation principal.
 */
data class MainSection(
    val block: TextBlock,
    val rawNumber: String,
    val normalizedNumber: String,   // Sans espaces/tirets : "AB123CD" ou "123ABC75"
    val system: RegistrationSystem
)