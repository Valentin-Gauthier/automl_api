package com.ecodrive.app.vehicle.plate.data.section

import com.ecodrive.app.vehicle.plate.extraction.TextBlock

/**
 * Section de validité temporaire : date MM/AA en rouge.
 */
data class ValiditySection(
    val block: TextBlock,
    val month: Int,
    val year: Int           // 2 chiffres
)