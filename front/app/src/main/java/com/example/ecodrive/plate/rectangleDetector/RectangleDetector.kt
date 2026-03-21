/**
 * This file is originated from https://github.com/smartbank-inc/android-rectangle-detector/blob/develop/library/src/main/java/jp/co/smartbank/rectangledetector/RectangleDetector.kt
 */
package com.example.ecodrive.plate.rectangleDetector

import android.graphics.Bitmap

/**
 * Detector for the four vertices of rectangles in a [Bitmap] graphic object.
 */
interface RectangleDetector {
    fun detect(bitmap: Bitmap): DetectionResult

    companion object {
        fun getInstance(detectionAccuracy: DetectionAccuracy = DetectionAccuracy.Passive): RectangleDetector {
            return RectangleDetectorImpl(detectionAccuracy)
        }
    }
}