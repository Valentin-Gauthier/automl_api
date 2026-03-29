package com.ecodrive.app.vehicle.plate.extraction

import android.graphics.PointF
import android.graphics.Rect

/**
 * Représente un bloc de texte détecté dans l'image.
 *
 * @param text        Texte brut reconnu
 * @param corners     4 coins du rectangle englobant (TL, TR, BR, BL)
 * @param boundingBox Rectangle englobant aligné sur les axes
 * @param textColor   Couleur dominante du texte (ARGB)
 * @param backgroundColor Couleur dominante du fond (ARGB)
 * @param confidence  Score de confiance ML Kit (0.0 – 1.0)
 *
 * Note : [TextBlock] n'est pas [android.os.Parcelable] intentionnellement.
 * Il reste interne au pipeline de scan ([TextExtractor] → [com.ecodrive.app.vehicle.plate.PlateAnalyzer])
 * et n'est jamais transféré entre Activities.
 */
data class TextBlock(
    val text: String,
    val corners: Array<PointF>, // [TL, TR, BR, BL]
    val boundingBox: Rect,
    val textColor: Int,
    val backgroundColor: Int,
    val confidence: Float
) {
    // equals / hashCode sur text + boundingBox uniquement (corners est un Array)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextBlock) return false
        return text == other.text && boundingBox == other.boundingBox
    }

    override fun hashCode(): Int = 31 * text.hashCode() + boundingBox.hashCode()
}
