package com.ecodrive.app.vehicle

enum class Carrosserie(
    override val code: Int?,
    override val label: String,
    override val description: String
) : SpinnerItem<Int> {
    UNKNOW(null, "Inconnue", "Carrosserie non reconnue"),

    FOURGON_BREAK(21, "Fourgon/Break", "Véhicule utilitaire ou break"),
    BERLINE_BICORPS(25, "Berline bicorps", "3 ou 5 portes"),
    TROIS_VOLUMES(27, "3 volumes", "Berline classique"),
    BREAK(28, "Break", "Voiture familiale"),
    COUPE(29, "Coupé", "Voiture 2 portes sportive"),
    DECAPOTABLE(30, "Décapotable", "Toit ouvrant ou amovible"),
    TARGA(31, "Targa", "Toit semi-amovible"),
    PICKUP(32, "Pick-up", "Véhicule avec benne"),
    BUS(33, "Autobus", "Transport collectif"),
    FOURGON(34, "Fourgon", "Véhicule utilitaire fermé"),
    TT_OUVERT(38, "Tout-terrain ouvert", "Carrosserie ouverte"),
    TT_FERME(39, "Tout-terrain fermé", "SUV/4x4 fermé"),
    MONOSPACE(40, "Monospace", "Grand véhicule familial"),
    PLATEFORME(42, "Plate-forme", "Camion châssis"),
    MUNICIPAL(46, "Municipal", "Usage collectivités"),
    CABINE_MOBILE(48, "Cabine mobile", "Structure mobile"),
    MOTO(51, "Moto", "Deux roues"),
    MIXTE_1(52, "Mixte", "Camionnette / berline"),
    SUV(53, "SUV", "Sport Utility Vehicle"),
    MIXTE_2(54, "Mixte SUV", "Camionnette / SUV"),
    MIXTE_3(55, "Mixte TT", "Camionnette / tout-terrain"),
    MIXTE_4(56, "Mixte monospace", "Camionnette / monospace");

    companion object {
        fun from(code: Int?): Carrosserie {
            return entries.find { it.code == code } ?: UNKNOW
        }
        fun codeFromLabel(label: String): Int? {
            return (entries.find { it.label == label } ?: UNKNOW).code
        }
    }
}