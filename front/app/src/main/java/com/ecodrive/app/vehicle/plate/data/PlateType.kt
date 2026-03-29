package com.ecodrive.app.vehicle.plate.data

/**
 * Type de plaque identifié par l'analyse visuelle et structurelle.
 */
enum class PlateType {
    UNKNOWN,
    NORMAL,
    NORMAL_ARRIERE,
    COLLECTION_RECENT,
    COLLECTION_OLD,
    TEMPORARY,
    SPECIAL;
}