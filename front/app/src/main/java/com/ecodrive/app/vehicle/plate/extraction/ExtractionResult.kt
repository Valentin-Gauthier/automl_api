package com.ecodrive.app.vehicle.plate.extraction

/**
 * Résultat complet de l'extraction sur une image.
 */
data class ExtractionResult(
    val blocks: List<TextBlock>,
    val imageWidth: Int,
    val imageHeight: Int
)