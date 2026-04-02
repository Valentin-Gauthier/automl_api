package com.ecodrive.app.vehicle.plate.data

/**
 * Système d'immatriculation déduit du format du numéro.
 */
enum class RegistrationSystem {
    UNKNOW,
    SIV,    // AB-123-CD (depuis 2009)
    FNI    // 123 ABC 75 (avant 2009)
}