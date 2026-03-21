/**
 * This file is originated from https://github.com/smartbank-inc/android-rectangle-detector/blob/develop/library/src/main/java/jp/co/smartbank/rectangledetector/dto/Rectangle.kt
 */
package com.example.ecodrive.plate.rectangleDetector

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Rectangle(
    val topLeft: Point,
    val topRight: Point,
    val bottomLeft: Point,
    val bottomRight: Point
) {
    private val TAG: String = "Rectangle"

    // get angle coordinates
    internal val points: List<Point>
        get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    // get rectangle dimensions
    private fun Point.distance(other: Point): Float {
        val diffX = abs(other.x - x).toFloat()
        val diffY = abs(other.y - y).toFloat()
        return sqrt((diffX * diffX) + (diffY * diffY))
    }

    private val topWidth: Float
        get() = topLeft.distance(topRight)
    private val bottomWidth: Float
        get() = bottomLeft.distance(bottomRight)
    private val leftHeight: Float
        get() = topLeft.distance(bottomLeft)
    private val rightHeight: Float
        get() = topRight.distance(bottomRight)

    internal val circumferenceLength: Float
        get() = topWidth + bottomWidth + leftHeight + rightHeight

    // get rectangle distortion
    internal val horizontalDistortionRatio: Float
        get() = if (leftHeight > rightHeight) {
            leftHeight / rightHeight
        } else {
            rightHeight / leftHeight
        }

    internal val verticalDistortionRatio: Float
        get() = if (topWidth > bottomWidth) {
            topWidth / bottomWidth
        } else {
            bottomWidth / topWidth
        }

    // Resize Rectangle.
    internal fun scaled(ratio: Float) = Rectangle(
        topLeft = Point(
            (topLeft.x * ratio),
            (topLeft.y * ratio)
        ),
        topRight = Point(
            (topRight.x * ratio),
            (topRight.y * ratio)
        ),
        bottomLeft = Point(
            (bottomLeft.x * ratio),
            (bottomLeft.y * ratio)
        ),
        bottomRight = Point(
            (bottomRight.x * ratio),
            (bottomRight.y * ratio)
        )
    )

    // Modify content of rectangle
    internal fun extractFrom(img: Bitmap, standardHeight: Int): Bitmap? {
        // Extract the portion of image from inner rectangle
        val xStart = min(topLeft.x, bottomLeft.x).roundToInt() // Most left
        val yStart = max(topLeft.y, topRight.y).roundToInt()   // Most top
        val xEnd = max(topRight.x, bottomRight.x).roundToInt() // Most right
        val yEnd = min(bottomLeft.y, bottomRight.y).roundToInt()//Most bottom
        // => Start in bottomLeft corner of glob rectangle
        // => End in topRight corner of glob rectangle

        val rectImg = try {
            Bitmap.createBitmap(img, xStart, yStart, xEnd - xStart, yEnd - yStart)
        } catch (e: Exception) {
            Log.e(TAG, "Error cropping potential plate", e)
            return null
        }

        // Correct distortion and size of the rectImg
        val width = min(topWidth, bottomWidth)
        val height = min(leftHeight, rightHeight)

        // Prepare source and destination points for perspective transform
        val srcPoints = MatOfPoint2f(
            topLeft,
            topRight,
            bottomRight,
            bottomLeft
        )
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(0.0, height.toDouble()),
            Point(height.toDouble(), width.toDouble()),
            Point(width.toDouble(), 0.0)
        )

        // Compute perspective transform
        val perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

        // Apply transforms
        val correctedMat = Mat()
        val inputMat = Mat()
        return try {
            Utils.bitmapToMat(rectImg, inputMat)
            Imgproc.warpPerspective(
                inputMat,
                correctedMat,
                perspectiveTransform,
                Size(width.toDouble(), height.toDouble())
            )

            // Convert corrected Mat to Bitmap
            val correctedBitmap = Bitmap.createBitmap(width.roundToInt(), height.roundToInt(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(correctedMat, correctedBitmap)

            // Resize to standard height
            val newWidth = (width * (standardHeight.toFloat() / height)).roundToInt()
            Bitmap.createScaledBitmap(correctedBitmap, newWidth, standardHeight, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error correcting distortion or resizing potential plate", e)
            null
        } finally {
            // Release OpenCV resources
            srcPoints.release()
            dstPoints.release()
            perspectiveTransform.release()
            correctedMat.release()
            inputMat.release()
        }
    }

    // Compare Rectangles
    internal fun isApproximated(other: Rectangle, distanceTolerance: Float): Boolean {
        return topLeft.distance(other.topLeft) <= distanceTolerance
                && topRight.distance(other.topRight) <= distanceTolerance
                && bottomLeft.distance(other.bottomLeft) <= distanceTolerance
                && bottomRight.distance(other.bottomRight) <= distanceTolerance
    }
    internal fun isIn(other: Rectangle): Boolean {
        //  <----   x   ++++>   /+\
        //  o.tL---------o.bR    |
        //   |    tL-tR   |      y
        //   |    bL-bT   |      |
        //  o.bL---------o.bR   \-/              _
        return topLeft.x >= other.topLeft.x    // \
                && topLeft.y <= other.topLeft.y//  \
                ///////////////////////////////       _
                && topRight.x <= other.topRight.x//  /
                && topRight.y <= other.topRight.y// /
                ///////////////////////////////
                && bottomLeft.x >= other.bottomLeft.x//   /
                && bottomLeft.y >= other.bottomLeft.y// _/
                ///////////////////////////////
                && bottomRight.x >= other.bottomRight.x// \
                && bottomRight.y <= other.bottomRight.y//  \_
                ///////////////////////////////
    }

    // Create rectangles
    companion object {
        private fun horizontalOrderPoints(a: Point, b: Point): Pair<Point, Point> {
            return if (a.x < b.x) Pair(a, b) else Pair(b, a)
        }
        fun from(points: List<Point>): Rectangle {
            require(points.size == 4)

            val sortedPoints = points.sortedBy { it.y }
            val (topLeft, topRight) = horizontalOrderPoints(sortedPoints[0], sortedPoints[1])
            val (bottomLeft, bottomRight) = horizontalOrderPoints(sortedPoints[2], sortedPoints[3])

            return Rectangle(topLeft, topRight, bottomLeft, bottomRight)
        }
    }
}
