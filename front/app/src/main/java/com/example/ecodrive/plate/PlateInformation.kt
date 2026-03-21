package com.example.ecodrive.plate

import com.example.ecodrive.plate.rectangleDetector.Rectangle
import java.time.YearMonth

data class PlateInformation(
    val number: String = "",
    val numberType: String = "",
    val territorialId: Int? = null,
    val endValidity: YearMonth? = null,
    val plateType: PlateType = PlateType.NOT_PLATE,
    val haveEuropeSection: Boolean = false,
    val position: Rectangle? = null
)