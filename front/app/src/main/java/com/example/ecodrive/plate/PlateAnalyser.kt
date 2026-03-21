package com.example.ecodrive.plate

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.ecodrive.plate.rectangleDetector.Rectangle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.YearMonth
import java.util.regex.Matcher
import java.util.regex.Pattern
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set

object PlateAnalyser {
    private const val TAG = "PlateAnalyser"
    // Text extractor
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private fun extractText(inputImage: InputImage): Pair<Boolean, Text> {
        lateinit var result: Text
        var success = true
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText -> result = visionText }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error processing OCR on potential plate", e)
                result = Text("", listOf<Any?>())
                success = false
            }
        return Pair(success, result)
    }

    private fun matcher(regex: String, text: String) : Matcher {
        return Pattern.compile(regex).matcher(text)
    }

    fun plateAnalyse(rectangle: Rectangle, image: Bitmap, standardHeight: Int) : Pair<Int, PlateInformation> {
        // Extract the image in the rectangle
        val rectangleImg = rectangle.extractFrom(image, standardHeight) ?: return Pair(0, PlateInformation())

        // Extract text of rectangle using OCR (ML Kit)
        val inputImage = InputImage.fromBitmap(rectangleImg, 0)
        val (success, texts) = extractText(inputImage)
        if (!success)
            return Pair(0, PlateInformation())

        // Initialization of variables
        var number = ""
        var numberType = ""
        var territorialId: Int? = null
        var endValidity: YearMonth? = null
        var plateType: PlateType = PlateType.NOT_PLATE
        var haveEuropeSection = false
        var score = 100

        // Analysis of textblock detected
        for (block in texts.textBlocks) {
            val (success, blockInfo) = colorAnalysis(rectangleImg, block)
            if (!success)
                continue
            val text = blockInfo.text

            // Classification of elements
            if (matcher("^[a-zA-Z][a-zA-Z]-[1-9][1-9][1-9]-[a-zA-Z][a-zA-Z]$", text).matches()) {
                // SIV of a normal (utility or collection vehicle) or temporary plate.
                val (valid, pT) = normalMatricule(plateType, blockInfo.colorsSet)
                if (valid) {
                    if (number != "")
                        score = 0// Immatriculation is already set. A plate can't have 2 immatriculation number.
                    else {
                        plateType = pT
                        number = text
                        numberType = "SIV"
                    }
                } else
                    score -= 10// Incompatibility color/type
            } else if (matcher("^[a-zA-Z][a-zA-Z] [1-9][1-9][1-9] [a-zA-Z][a-zA-Z]$", text).matches()) {
                // FNI of a normal (utility or collection vehicle) or temporary plate.
                val (valid, pT) = normalMatricule(plateType, blockInfo.colorsSet)
                if (valid) {
                    if (number != "")
                        score = 0// Immatriculation is already set. A plate can't have 2 immatriculation number.
                    else {
                        plateType = pT
                        number = text
                        numberType = "FNI"
                    }
                } else
                    score-= 10// Incompatibility color/type
            } else if (matcher("^([1-9]{1-3}|[az-AZ]{1-3})[ -]([1-9]{1-3}|[az-AZ]{1-3})[ -]([1-9]{1-3}|[az-AZ]{1-3})$", text).matches()) {
                // Matricule of special vehicle
                if ((blockInfo.colorsSet == ColorSets.SPECIAL)
                    && (plateType == PlateType.NOT_PLATE || plateType == PlateType.SPECIAL)
                ) {
                    if (number != "")
                        score = 0// Immatriculation is already set. A plate can't have 2 immatriculation number.
                    else {
                        number = text
                        numberType =
                            if (blockInfo.specialColorsSet == SpecialColorSets.CORP_DIPLOMACY)
                                "SPECIAL-CorpDiplomacy"
                            else {
                                score-= 1// Unknow type of plate of special vehicle
                                "SPECIAL"
                            }
                        plateType = PlateType.SPECIAL
                    }
                } else
                    score-= 10// Incompatibility color/type
            } else if (matcher("^[a-zA-Z]$", text).matches()) {
                // Country code of the europe section,
                val (valid, pT) = borderSection(plateType, blockInfo.colorsSet)
                if (valid && !haveEuropeSection) {
                    plateType = pT
                    haveEuropeSection = true
                } else
                    score-= 5// 2 european section ? Or incompatible european section ?
            } else if (matcher("^[1-9][1-9]$", text).matches()) {
                // Short number => territorial identification
                val (valid, pT) = borderSection(plateType, blockInfo.colorsSet)
                if (valid && territorialId == null) {
                    plateType = pT
                    territorialId = text.toInt()
                } else
                    score-= 5// 2 ti section ? Or incompatible ti section ?
            } else {
                val m = matcher("^[1-9][1-9]\n[1-9][1-9]$", text)
                if (m.matches()) {
                    // Short number => end validity date
                    if ((blockInfo.colorsSet == ColorSets.TEMPORARY)
                        && (plateType == PlateType.NOT_PLATE || plateType == PlateType.TEMPORARY)
                    ) {
                        endValidity?.let {
                            score-= 5// 2 evd section ? Or incompatible evd section ?
                        } ?: {
                            plateType = PlateType.TEMPORARY
                            val validityMonth = m.group(0)?.toInt()
                            val validityYear = m.group(1)?.toInt()
                            validityYear?.let {
                                validityMonth?.let {
                                    endValidity = YearMonth.of(validityYear, validityMonth)
                                }
                            }
                        }
                    } else
                        score-= 10// Incompatibility color/type
                }
            }
        }


        if (number.isEmpty())
            score = 0
        else {
            when (plateType) {
                PlateType.NOT_PLATE -> {
                    plateType = PlateType.UNKNOW
                    score/= 10
                }
                PlateType.UNKNOW -> score/= 10
                PlateType.SPECIAL -> score/= 5
                PlateType.COLLECTION -> {
                    score+= calcScoreOfNormalMatricule(haveEuropeSection, numberType, territorialId)
                    score/= 2
                }
                PlateType.UTILITARY ->
                    score+= calcScoreOfNormalMatricule(haveEuropeSection, numberType, territorialId)
                PlateType.TEMPORARY -> score/= 2
            }
        }

        score = minOf(0, score)
        score = maxOf(100, score)

        return Pair(score, PlateInformation(
            number,
            numberType,
            territorialId,
            endValidity,
            plateType,
            haveEuropeSection,
            rectangle
        ))
    }

    private fun calcScoreOfNormalMatricule(haveEuropeSection: Boolean, numberType: String, territorialId: Int?): Int {
        var score = 0
        if (haveEuropeSection)
            score+= 5//Full information
        else
            score-= 5//Miss obligatory information
        if (numberType == "SIV") {
            if (territorialId == null)
                score-= 5//Miss obligatory information
            else
                score+= 5//Full information
        } else {
            if (territorialId == null)
                score+= 5//Full information
            else
                score-= 1//Over information
        }
        return score
    }

    private fun normalMatricule(plateType: PlateType, colorsSet: ColorSets): Pair<Boolean, PlateType> {
        val newPlateType: PlateType = if ((colorsSet == ColorSets.NORMAL)
            && (plateType == PlateType.NOT_PLATE || plateType == PlateType.UTILITARY || plateType == PlateType.COLLECTION)
        )
            PlateType.UTILITARY
        else if ((colorsSet == ColorSets.COLLECTION)
            && (plateType == PlateType.NOT_PLATE || plateType == PlateType.COLLECTION)
        )
            PlateType.COLLECTION
        else if ((colorsSet == ColorSets.TEMPORARY)
            && (plateType == PlateType.NOT_PLATE || plateType == PlateType.TEMPORARY)
        )
            PlateType.TEMPORARY
        else
            return Pair(false, plateType)
        return Pair(true, newPlateType)
    }

    private fun borderSection(plateType: PlateType, colorsSet: ColorSets): Pair<Boolean, PlateType> {
        val newPlateType: PlateType = if ((colorsSet == ColorSets.EUROPEAN_SECTION)
            && (plateType == PlateType.NOT_PLATE || plateType == PlateType.UTILITARY)
        )
            PlateType.UTILITARY
        else if ((colorsSet == ColorSets.COLLECTION)
            && (plateType == PlateType.NOT_PLATE || plateType == PlateType.COLLECTION)
        )
            PlateType.COLLECTION
        else
            return Pair(false, plateType)
        return Pair(true, newPlateType)
    }

    private enum class ColorSets {
        UNKNOW,
        NORMAL,
        COLLECTION,
        TEMPORARY,
        EUROPEAN_SECTION,
        SPECIAL
    }
    private enum class SpecialColorSets {
        UNKNOW,
        CORP_DIPLOMACY
    }
    private data class TextBlockInfo (
        val text: String = "",
        val bgColor: ColorClass = ColorClass.BLACK,
        val textColor: ColorClass = ColorClass.BLACK,
        var colorsSet: ColorSets = ColorSets.UNKNOW,
        var specialColorsSet: SpecialColorSets? = null,
    )

    private fun colorAnalysis(img: Bitmap, blockText: Text.TextBlock): Pair<Boolean, TextBlockInfo> {
        // Get text of element and color of this portion of text and her background color
        val text = blockText.text
        val boundingBox = blockText.boundingBox ?: return Pair(false, TextBlockInfo(text))

        // Zoom on text space
        val textImg = try {
            Bitmap.createBitmap(
                img,
                boundingBox.left,
                boundingBox.top,
                boundingBox.width(),
                boundingBox.height()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cropping OCR element", e)
            return Pair(false, TextBlockInfo(text))
        }

        // Compress image for reduce at 16 colors
        val textImgCompress = try {
            // Reduce resolution for accelerate calculates
            val smallImg = textImg.scale(32, 32)
            // Compress colors
            compressTo16Colors(smallImg)
        } catch (e: Exception) {
            Log.e(TAG, "Error classification colors of image", e)
            return Pair(false, TextBlockInfo(text))
        }

        // Find the two dominants colors
        val (bgColor, textColor) = getPlateSectionColors(textImgCompress)

        // Analysis color composition
        val textBlockInfo = TextBlockInfo(text, bgColor, textColor)
        if (bgColor == ColorClass.WHITE && textColor == ColorClass.BLACK)
            textBlockInfo.colorsSet = ColorSets.NORMAL
        else if (bgColor == ColorClass.BLACK && textColor == ColorClass.WHITE)
            textBlockInfo.colorsSet = ColorSets.COLLECTION
        else if (bgColor == ColorClass.RED && textColor == ColorClass.WHITE)
            textBlockInfo.colorsSet = ColorSets.TEMPORARY
        else if (bgColor == ColorClass.BLUE && textColor == ColorClass.WHITE)
            textBlockInfo.colorsSet = ColorSets.EUROPEAN_SECTION
        else {
            textBlockInfo.colorsSet = ColorSets.SPECIAL
            textBlockInfo.specialColorsSet = SpecialColorSets.UNKNOW
            // Special sets of colors
            if (bgColor == ColorClass.GREEN && (textColor == ColorClass.ORANGE || textColor == ColorClass.LIGHT_GREY))// SILVERING
                textBlockInfo.specialColorsSet = SpecialColorSets.CORP_DIPLOMACY
        }
        // Unknow combination of colors => Unknow special plate
        return Pair(true, textBlockInfo)
    }

    // Compressed from 255^3 colors to 16 colors
    private fun compressTo16Colors(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val compressedBitmap = createBitmap(width, height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap[x, y]
                val r = (Color.red(pixel) shr 7) shl 7  // Reduce at 0 or 128
                val g = (Color.green(pixel) shr 7) shl 7
                val b = (Color.blue(pixel) shr 7) shl 7
                compressedBitmap[x, y] = Color.rgb(r, g, b)
            }
        }

        return compressedBitmap
    }

    // Extract plate section colors
    private fun getPlateSectionColors(bitmap: Bitmap): Pair<ColorClass, ColorClass> {
        val colorCounts = mutableMapOf<Int, Int>()

        // Count number of use of each colors
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap[x, y]
                colorCounts[pixel] = colorCounts.getOrDefault(pixel, 0) + 1
            }
        }

        // Sort colors by frequencies of use
        val sortedColors = colorCounts.toList().sortedByDescending { it.second }

        // Select the two colors was the most used
        val dominantColor1 = if (sortedColors.isNotEmpty()) sortedColors[0].first else Color.BLACK
        val dominantColor2 = if (sortedColors.size > 1) sortedColors[1].first else Color.WHITE

        // Name colors
        val bgColorClass = classifyColor(dominantColor1)
        val textColorClass = classifyColor(dominantColor2)

        // Select color of background and color of text
        return if (Color.luminance(dominantColor1) > Color.luminance(dominantColor2)) {
            Pair(bgColorClass, textColorClass)
        } else {
            Pair(textColorClass, bgColorClass)
        }
    }

    private fun classifyColor(color: Int): ColorClass {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        // Calculer la luminosité
        val luminosity = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

        // Calculer la saturation
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val saturation = if (max == 0) 0 else (max - min) * 255 / max

        // Classer en gris si la saturation est faible
        if (saturation < 30) {
            return when {
                luminosity < 30 -> ColorClass.BLACK
                luminosity < 100 -> ColorClass.DARK_GREY
                luminosity < 200 -> ColorClass.LIGHT_GREY
                else -> ColorClass.WHITE
            }
        }

        // Classer en couleur sinon
        return when {
            r == max && g < 100 && b < 100 -> ColorClass.RED
            g == max && r < 100 && b < 100 -> ColorClass.GREEN
            b == max && r < 100 && g < 100 -> ColorClass.BLUE
            r == max && g > 100 && b < 100 -> ColorClass.ORANGE
            r == max && b > 100 && g < 100 -> ColorClass.MAGENTA
            g == max && b > 100 && r < 100 -> ColorClass.CYAN
            r > 150 && g > 150 && b < 100 -> ColorClass.YELLOW
            r > 150 && b > 150 && g < 100 -> ColorClass.ROSE
            g > 150 && b > 150 && r < 100 -> ColorClass.AZURE
            r > 100 && b > 100 && g < 100 -> ColorClass.VIOLET
            else -> ColorClass.WHITE
        }
    }

    // Classer une couleur dans l'une des 16 catégories
    private enum class ColorClass {
        // GrayScale
        BLACK, DARK_GREY, LIGHT_GREY, WHITE,
        // RGB primary
        RED, GREEN, BLUE,
        // RGB secondary
        YELLOW, CYAN, MAGENTA,
        // RGB tertiary
        ORANGE, AZURE, VIOLET, ROSE
    }
}