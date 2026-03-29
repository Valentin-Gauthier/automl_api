package com.ecodrive.app.vehicle

enum class GearboxType(
    override val code: String?,
    override val label: String,
    override val description: String
) : SpinnerItem<String> {
    UNKNOW(null, "Inconnu", "Type de boîte non reconnu"),

    AUTOMATIQUE("A", "Automatique", "Boîte automatique"),
    MANUELLE("M", "Manuelle", "Boîte manuelle"),
    SEQUENTIELLE("S", "Séquentielle", "Boîte séquentielle"),
    CVT("V", "CVT", "Transmission à variation continue"),
    ROBOTISEE("X", "Robotisée", "Boîte manuelle robotisée");

    companion object {
        fun from(code: String?): GearboxType {
            return entries.find { it.code == code } ?: UNKNOW
        }
        fun codeFromLabel(label: String): String? {
            return (entries.find { it.label == label } ?: UNKNOW).code
        }
    }
}