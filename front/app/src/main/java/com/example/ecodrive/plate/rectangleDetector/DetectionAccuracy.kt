package com.example.ecodrive.plate.rectangleDetector

import com.example.ecodrive.plate.rectangleDetector.strategy.AdaptiveThresholdStrategy
import com.example.ecodrive.plate.rectangleDetector.strategy.CannyAlgorithmStrategy
import com.example.ecodrive.plate.rectangleDetector.strategy.CompositeContourDetectionStrategy
import com.example.ecodrive.plate.rectangleDetector.strategy.ContourDetectionStrategy

/**
 * Mode of accuracy of rectangle detection in [RectangleDetector].
 */
enum class DetectionAccuracy {
    /**
     * [DetectionAccuracy] that prioritizes the number of detections over the accuracy of the results.
     * (for user command)
     */
    Aggressive,

    /**
     * [DetectionAccuracy] that prioritizes the accuracy of the results.
     * (for cron command)
     */
    Passive;

    internal fun buildContourStrategy(): ContourDetectionStrategy = when (this) {
        Aggressive -> CompositeContourDetectionStrategy(
            listOf(
                AdaptiveThresholdStrategy(),
                CannyAlgorithmStrategy(CannyAlgorithmStrategy.Level.Normal),
                CannyAlgorithmStrategy(CannyAlgorithmStrategy.Level.Strict)
            )
        )
        Passive -> CannyAlgorithmStrategy(CannyAlgorithmStrategy.Level.Normal)
    }
}