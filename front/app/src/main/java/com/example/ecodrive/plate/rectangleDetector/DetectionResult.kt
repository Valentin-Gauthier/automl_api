/**
 * This file is originated from https://github.com/smartbank-inc/android-rectangle-detector/blob/develop/library/src/main/java/jp/co/smartbank/rectangledetector/dto/DetectionResult.kt
 */
package com.example.ecodrive.plate.rectangleDetector

import android.graphics.Bitmap

/**
 * An object containing detected [Rectangle]s.
 */
data class DetectionResult(
    val image: Bitmap,
    val rectangles: List<Rectangle> = emptyList()
)