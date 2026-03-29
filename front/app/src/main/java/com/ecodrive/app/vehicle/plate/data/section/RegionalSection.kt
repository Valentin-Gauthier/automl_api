package com.ecodrive.app.vehicle.plate.data.section

import com.ecodrive.app.vehicle.plate.extraction.TextBlock

/**
 * Section régionale : bandeau droit avec numéro de département + logo région.
 */
data class RegionalSection(
    val block: TextBlock,
    val departmentCode: String,
    val isBlue: Boolean     // true = bleu, false = noir (collection récente)
)