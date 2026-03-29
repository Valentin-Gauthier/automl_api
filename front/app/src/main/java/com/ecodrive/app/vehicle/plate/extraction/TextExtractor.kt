package com.ecodrive.app.vehicle.plate.extraction

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.core.graphics.get

/**
 * Extrait tous les blocs de texte d'une image (Bitmap ou InputImage).
 */
class TextExtractor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Extrait les blocs de texte de façon asynchrone.
     * @param bitmap  Image source
     * @param onResult  Callback appelé avec le résultat (thread principal)
     * @param onError   Callback appelé en cas d'erreur
     */
    fun extract(
        bitmap: Bitmap,
        onResult: (ExtractionResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.flatMap { block ->
                    block.lines.mapNotNull { line ->
                        buildTextBlock(line, bitmap)
                    }
                }
                onResult(ExtractionResult(blocks, bitmap.width, bitmap.height))
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun release() {
        recognizer.close()
    }

    // -------------------------------------------------------------------------
    // Construction d'un TextBlock
    // -------------------------------------------------------------------------

    private fun buildTextBlock(
        line: Text.Line,
        bitmap: Bitmap
    ): TextBlock? {
        val box = line.boundingBox ?: return null

        // Coins : ML Kit fournit un tableau de Point, on les convertit en PointF
        val rawCorners = line.cornerPoints ?: arrayOf()
        val corners: Array<PointF> = if (rawCorners.size == 4) {
            rawCorners.map { PointF(it.x.toFloat(), it.y.toFloat()) }.toTypedArray()
        } else {
            // Fallback : on génère les 4 coins depuis le bounding box
            arrayOf(
                PointF(box.left.toFloat(),  box.top.toFloat()),
                PointF(box.right.toFloat(), box.top.toFloat()),
                PointF(box.right.toFloat(), box.bottom.toFloat()),
                PointF(box.left.toFloat(),  box.bottom.toFloat())
            )
        }

        val safeBox = clampRect(box, bitmap.width, bitmap.height)
        val (textColor, bgColor) = sampleColors(bitmap, safeBox)
        val confidence = line.elements
            .mapNotNull { it.confidence }
            .average()
            .let { if (it.isNaN()) 0f else it.toFloat() }

        return TextBlock(
            text = line.text,
            corners = corners,
            boundingBox = safeBox,
            textColor = textColor,
            backgroundColor = bgColor,
            confidence = confidence
        )
    }

    // -------------------------------------------------------------------------
    // Analyse des couleurs
    // -------------------------------------------------------------------------

    /**
     * Échantillonne les pixels du bloc pour distinguer couleur de texte et de fond.
     *
     * Stratégie :
     *   1. On prend une grille de pixels dans le bounding box.
     *   2. On les regroupe par luminosité (bimodale) : sombres → texte, clairs → fond.
     *      Sauf si le fond est sombre (ex: plaque noire) : on inverse.
     *   3. On calcule la couleur moyenne de chaque groupe.
     *
     * @return Pair(textColor, backgroundColor) en ARGB
     */
    private fun sampleColors(bitmap: Bitmap, box: Rect): Pair<Int, Int> {
        val pixels = mutableListOf<Int>()
        val stepX = maxOf(1, box.width()  / SAMPLE_GRID)
        val stepY = maxOf(1, box.height() / SAMPLE_GRID)

        for (y in box.top until box.bottom step stepY) {
            for (x in box.left until box.right step stepX) {
                pixels.add(bitmap[x, y])
            }
        }

        if (pixels.isEmpty()) return Pair(Color.BLACK, Color.WHITE)

        // Sépare pixels sombres / clairs par rapport à la luminance médiane
        val luminances = pixels.map { luminance(it) }
        val median = luminances.sorted()[luminances.size / 2]

        val darkPixels  = pixels.filterIndexed { i, _ -> luminances[i] <  median }
        val lightPixels = pixels.filterIndexed { i, _ -> luminances[i] >= median }

        val darkAvg  = averageColor(darkPixels)
        val lightAvg = averageColor(lightPixels)

        // Le fond est la couleur majoritaire
        return if (lightPixels.size >= darkPixels.size) {
            Pair(darkAvg, lightAvg)   // texte sombre sur fond clair
        } else {
            Pair(lightAvg, darkAvg)   // texte clair sur fond sombre
        }
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color)   / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color)  / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun averageColor(pixels: List<Int>): Int {
        if (pixels.isEmpty()) return Color.GRAY
        val r = pixels.map { Color.red(it)   }.average().toInt()
        val g = pixels.map { Color.green(it) }.average().toInt()
        val b = pixels.map { Color.blue(it)  }.average().toInt()
        return Color.rgb(r, g, b)
    }

    private fun clampRect(rect: Rect, maxW: Int, maxH: Int): Rect = Rect(
        rect.left.coerceIn(0, maxW - 1),
        rect.top.coerceIn(0, maxH - 1),
        rect.right.coerceIn(1, maxW),
        rect.bottom.coerceIn(1, maxH)
    )

    companion object {
        /** Grille d'échantillonnage NxN par bloc */
        private const val SAMPLE_GRID = 8
    }
}