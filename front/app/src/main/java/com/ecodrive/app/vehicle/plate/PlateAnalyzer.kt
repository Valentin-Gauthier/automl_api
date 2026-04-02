package com.ecodrive.app.vehicle.plate

import android.graphics.Color
import android.graphics.Rect
import com.ecodrive.app.vehicle.plate.data.section.EuSection
import com.ecodrive.app.vehicle.plate.data.section.MainSection
import com.ecodrive.app.vehicle.plate.data.PlateFormat
import com.ecodrive.app.vehicle.plate.data.PlateInformation
import com.ecodrive.app.vehicle.plate.data.PlateInformationBuilder
import com.ecodrive.app.vehicle.plate.data.PlateType
import com.ecodrive.app.vehicle.plate.data.section.RegionalSection
import com.ecodrive.app.vehicle.plate.data.RegistrationSystem
import com.ecodrive.app.vehicle.plate.data.section.ValiditySection
import com.ecodrive.app.vehicle.plate.extraction.ExtractionResult
import com.ecodrive.app.vehicle.plate.extraction.TextBlock

/**
 * Analyse les blocs de texte produits par [com.ecodrive.app.vehicle.plate.extraction.TextExtractor] et construit
 * un [PlateInformation] Parcelable (sans TextBlock ni Rect internes).
 */
class PlateAnalyzer {

    // =========================================================================
    // API publique
    // =========================================================================

    /**
     * Point d'entrée principal.
     *
     * @param result  Résultat de [com.ecodrive.app.vehicle.plate.extraction.TextExtractor.extract]
     * @return [PlateInformation] — toujours non-null ; vérifier [PlateInformationBuilder.isValid]
     */
    fun analyze(result: ExtractionResult): PlateInformation {
        val blocks = result.blocks

        // 1. Cherche un numéro d'immatriculation valide parmi les blocs
        val mainBlock = findMainSection(blocks) ?:
            return PlateInformation.new()

        // 2. Identifie les blocs voisins (EU, région, validité)
        val euBlock       = findEuSection(blocks, mainBlock)
        val regionBlock   = findRegionalSection(blocks, mainBlock)
        val validityBlock = findValiditySection(blocks, mainBlock)

        // 3. Construit les sections
        val mainSection     = buildMainSection(mainBlock)
        val euSection       = euBlock?.let { buildEuSection(it) }
        val regionalSection = regionBlock?.let { buildRegionalSection(it) }
        val validitySection = validityBlock?.let { buildValiditySection(it) }

        // 4. Calcule le rectangle de la plaque (union de tous les blocs participants)
        val participants = listOfNotNull(mainBlock, euBlock, regionBlock, validityBlock)
        val plateArea    = unionRect(participants.map { it.boundingBox })

        // 5. Déduit le format physique
        val format = detectFormat(plateArea)

        // 6. Classifie le type
        val type = classifyType(
            mainSection,
            euSection,
            regionalSection,
            mainBlock
        )

        // 7. Score de confiance global
        val confidence = computeConfidence(
            mainBlock,
            euBlock,
            regionBlock,
            type,
            format
        )

        // 8. Result
        return PlateInformationBuilder(
            type = type,
            format = format,
            plateArea = plateArea,
            euSection = euSection,
            regionalSection = regionalSection,
            mainSection = mainSection,
            validitySection = validitySection,
            confidence = confidence,
            rawBlocks = participants
        ).build()
    }

    // =========================================================================
    // Détection des sections
    // =========================================================================

    /** Trouve le bloc qui contient un numéro d'immatriculation valide. */
    private fun findMainSection(blocks: List<TextBlock>): TextBlock? =
        blocks.firstOrNull {
            SIV_REGEX.containsMatchIn(it.text) ||
            FNI_REGEX.containsMatchIn(it.text)
        }

    /**
     * Trouve la section EU : bloc à gauche du numéro, contenant "F",
     * fond bleu ou noir, largeur beaucoup plus petite que la section principale.
     */
    private fun findEuSection(blocks: List<TextBlock>, main: TextBlock): TextBlock? {
        val mainBox = main.boundingBox
        return blocks
            .filter { it != main }
            .filter { block ->
                val box = block.boundingBox
                // À gauche du numéro
                box.right <= mainBox.left + TOLERANCE_PX &&
                    // Même zone verticale
                    verticalOverlap(box, mainBox) > 0.4f &&
                    // Contient "F" ou "EU" ou les étoiles
                    block.text.contains(Regex("[Ff]|EU|\\*"))
            }
            .minByOrNull { it.boundingBox.left }   // le plus à gauche
    }

    /**
     * Trouve la section régionale : bloc à droite du numéro,
     * contient 2 chiffres de département (01–99, 2A, 2B).
     */
    private fun findRegionalSection(blocks: List<TextBlock>, main: TextBlock): TextBlock? {
        val mainBox = main.boundingBox
        return blocks
            .filter { it != main }
            .filter { block ->
                val box = block.boundingBox
                box.left >= mainBox.right - TOLERANCE_PX &&
                    verticalOverlap(box, mainBox) > 0.4f &&
                    DEPT_REGEX.containsMatchIn(block.text)
            }
            .minByOrNull { it.boundingBox.left }
    }

    /**
     * Trouve la section de validité temporaire : bloc fond rouge,
     * contient un pattern MM/AA ou 4 chiffres disposés en 2+2.
     */
    private fun findValiditySection(blocks: List<TextBlock>, main: TextBlock): TextBlock? {
        val mainBox = main.boundingBox
        return blocks
            .firstOrNull { it != main
                    && isRed(it.backgroundColor)
                    && VALIDITY_REGEX.containsMatchIn(it.text)
                    && verticalOverlap(it.boundingBox, mainBox) > 0.2f
            }
    }

    // =========================================================================
    // Construction des sections
    // =========================================================================

    private fun buildMainSection(block: TextBlock): MainSection {
        val text   = block.text.trim()
        val isSIV  = SIV_REGEX.containsMatchIn(text)
        val raw    = if (isSIV) SIV_REGEX.find(text)!!.value
        else       FNI_REGEX.find(text)!!.value
        val normalized = raw.replace(Regex("[\\s\\-]"), "").uppercase()
        return MainSection(
            block     = block,
            rawNumber = raw,
            normalizedNumber = normalized,
            system    = if (isSIV) RegistrationSystem.SIV else RegistrationSystem.FNI
        )
    }

    private fun buildEuSection(block: TextBlock) =
        EuSection(
            block = block,
            isBlue = isBlue(block.backgroundColor)
        )

    private fun buildRegionalSection(block: TextBlock) =
        RegionalSection(
            block  = block,
            departmentCode = DEPT_REGEX.find(block.text)?.value ?: "??",
            isBlue = isBlue(block.backgroundColor)
        )

    private fun buildValiditySection(block: TextBlock): ValiditySection? {
        val match = VALIDITY_REGEX.find(block.text) ?: return null
        val month = match.groupValues[1].toIntOrNull() ?: return null
        val year  = match.groupValues[2].toIntOrNull() ?: return null
        return ValiditySection(block = block, month = month, year = year)
    }

    // =========================================================================
    // Classification
    // =========================================================================

    private fun classifyType(
        main: MainSection,
        eu: EuSection?,
        region: RegionalSection?,
        mainBlock: TextBlock
    ): PlateType {
        val hasEu     = eu != null
        val euIsBlue  = eu?.isBlue ?: false
        val euIsBlack = hasEu && !euIsBlue

        val hasRegion     = region != null
        val regionIsBlue  = region?.isBlue ?: false
        val regionIsBlack = hasRegion && !regionIsBlue

        val bgColor = mainBlock.backgroundColor

        return when {
            // Plaque temporaire (WW) : fond rouge + date?
            isRed(bgColor)
                -> PlateType.TEMPORARY
            // Collection récente : bandeaux noirs
            euIsBlack && regionIsBlack && isWhite(bgColor)
                -> PlateType.COLLECTION_RECENT
            // Collection ancienne : pas de bandeau EU, fond noir
            !hasEu && isBlack(bgColor)
                -> PlateType.COLLECTION_OLD
            // FNI fond jaune (plaque arrière)
            main.system == RegistrationSystem.FNI && isYellow(bgColor)
                -> PlateType.NORMAL_ARRIERE
            // FNI fond blanc
            main.system == RegistrationSystem.FNI && (isWhite(bgColor) || euIsBlue)
                -> PlateType.NORMAL
            // SIV normal : EU bleu + région bleue + fond blanc
            euIsBlue && regionIsBlue && isWhite(bgColor)
                -> PlateType.NORMAL
            // SIV sans région (cas limite, probablement normal)
            euIsBlue && !hasRegion && isWhite(bgColor)
                -> PlateType.NORMAL
            else -> PlateType.UNKNOW
        }
    }

    // =========================================================================
    // Format physique
    // =========================================================================

    private fun detectFormat(plateRect: Rect): PlateFormat {
        if (plateRect.isEmpty) return PlateFormat.UNKNOW
        val ratio = plateRect.width().toFloat() / plateRect.height()
        // Moto : plaque quasi carrée ou portrait (ratio < 2.0)
        // Voiture : très allongée (ratio > 3.0)
        return when {
            ratio < MOTO_RATIO_THRESHOLD -> PlateFormat.MOTORCYCLE
            ratio > CAR_RATIO_THRESHOLD  -> PlateFormat.STANDARD
            else                         -> PlateFormat.UNKNOW
        }
    }

    // =========================================================================
    // Confiance
    // =========================================================================

    private fun computeConfidence(
        main: TextBlock,
        eu: TextBlock?,
        region: TextBlock?,
        type: PlateType,
        format: PlateFormat
    ): Float {
        // Section EU présente
        var score = 0.2f * (if (eu  != null) 1 else -1)

        // Section région présente
        score+= 0.15f * (if (region != null) 1 else -1)

        // Type résolu (pas UNKNOWN)
        score+= 0.10f * (if (type   != PlateType.UNKNOW) 1 else -1)

        // Format résolu (pas UNKNOWN)
        score+= 0.05f * (if (format != PlateFormat.UNKNOW) 1 else -1)

        // Calcul de la probabilité d'avoir trouvé un numéro d'immatriculation
        score = main.confidence * (0.5f + score)
        return score.coerceIn(0f, 1f)
    }

    // =========================================================================
    // Géométrie
    // =========================================================================

    private fun verticalOverlap(a: Rect, b: Rect): Float {
        val overlapTop    = maxOf(a.top, b.top)
        val overlapBottom = minOf(a.bottom, b.bottom)
        val overlap       = (overlapBottom - overlapTop).toFloat()
        val minHeight     = minOf(a.height(), b.height()).toFloat()
        return if (minHeight <= 0) 0f else (overlap / minHeight).coerceIn(0f, 1f)
    }

    private fun unionRect(rects: List<Rect>) = Rect(
        rects.minOf { it.left },
        rects.minOf { it.top },
        rects.maxOf { it.right },
        rects.maxOf { it.bottom }
    )

    // =========================================================================
    // Utilitaires couleur
    // =========================================================================

    /** Teinte H en degrés dans HSV */
    private fun hsv(color: Int) = FloatArray(3).also {
        Color.colorToHSV(color, it)
    }
    private fun hue(color: Int)        = hsv(color)[0]
    private fun saturation(color: Int) = hsv(color)[1]
    private fun value(color: Int)      = hsv(color)[2]

    /** Bleu EU : teinte ~210-250°, saturation élevée */
    private fun isBlue(color: Int): Boolean {
        val h = hue(color); val s = saturation(color); val v = value(color)
        return h in 190f..265f && s > 0.40f && v > 0.15f
    }

    /** Noir : value très basse */
    private fun isBlack(color: Int): Boolean = value(color) < 0.20f

    /** Blanc : saturation très basse + value élevée */
    private fun isWhite(color: Int): Boolean =
        saturation(color) < 0.20f && value(color) > 0.80f

    /** Jaune : teinte ~45-70° */
    private fun isYellow(color: Int): Boolean {
        val h = hue(color); val s = saturation(color)
        return h in 40f..75f && s > 0.50f
    }

    /** Rouge : teinte <15° ou >340° */
    private fun isRed(color: Int): Boolean {
        val h = hue(color); val s = saturation(color)
        return (h !in 15f..340f) && s > 0.45f
    }

    // =========================================================================
    // Constantes
    // =========================================================================

    companion object {
        /** Format SIV : AB-123-CD */
        val SIV_REGEX = Regex("[A-Za-z]{2}[-\\s]?\\d{3}[-\\s]?[A-Za-z]{2}")

        /** Format FNI : 123 ABC 75 ou 123ABC75 */
        val FNI_REGEX = Regex("\\d{1,4}[-\\s]?[A-Za-z]{2,3}[-\\s]?\\d{2}")

        /** Département : 01–99, 2A, 2B */
        private val DEPT_REGEX = Regex("\\b(2[AB]|[0-9]{2})\\b")

        /** Validité WW : MM/AA ou MM AA */
        private val VALIDITY_REGEX = Regex("(\\d{2})[-/\\s](\\d{2})")

        /** Tolérance de position (px) pour associer les blocs voisins */
        private const val TOLERANCE_PX = 30

        /** Ratio largeur/hauteur en-dessous duquel on considère la plaque comme moto */
        private const val MOTO_RATIO_THRESHOLD = 2.0f

        /** Ratio largeur/hauteur au-dessus duquel on considère la plaque comme voiture */
        private const val CAR_RATIO_THRESHOLD = 3.0f
    }
}