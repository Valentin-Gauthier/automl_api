/**
 * This file is originated from https://github.com/smartbank-inc/android-rectangle-detector/blob/develop/library/src/main/java/jp/co/smartbank/rectangledetector/strategy/CompositeContourDetectionStrategy.kt
 */
package com.example.ecodrive.plate.rectangleDetector.strategy

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint

/**
 * [ContourDetectionStrategy] that represents a group of strategies.
 */
class CompositeContourDetectionStrategy(
    private val strategies: List<ContourDetectionStrategy>
) : ContourDetectionStrategy() {
    override fun detectContours(originalImageMat: Mat): List<MatOfPoint> {
        return strategies.map { it.detectContours(originalImageMat) }.flatten()
    }
}