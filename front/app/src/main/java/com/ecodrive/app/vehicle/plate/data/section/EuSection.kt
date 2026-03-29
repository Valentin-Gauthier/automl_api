package com.ecodrive.app.vehicle.plate.data.section

import com.ecodrive.app.vehicle.plate.extraction.TextBlock

/**
 * Section EU : bandeau gauche avec drapeau/étoiles + code pays.
 */
data class EuSection(
    val block: TextBlock,
    val isBlue: Boolean,    // true = bleu, false = noir (collection récente)
    val countryCode: String = "F"
)