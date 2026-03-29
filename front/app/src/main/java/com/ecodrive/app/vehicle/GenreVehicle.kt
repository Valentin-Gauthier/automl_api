package com.ecodrive.app.vehicle

enum class GenreVehicle(
    override val code: Int?,
    override val label: String,
    override val description: String
) : SpinnerItem<Int> {
    UNKNOW(null, "Inconnu", "Genre de véhicule non reconnu"),

    VP(1, "VP", "Véhicule particulier"),
    CTTE(2, "CTTE", "Camionnette"),
    VASP(3, "VASP", "Véhicule automoteur spécialisé"),
    CAM(4, "CAM", "Camion"),
    MOTO(5, "Moto", "Motocyclette (MTL, MTT1, MTT2, MTTE, MOTO)"),
    CL(6, "Cyclomoteur", "Vélomoteur ≤ 50 cm³"),
    QM(7, "Quadricycle", "Quadricycles à moteur (quad, voiturette)"),
    AGRI(8, "Engin agricole", "Engin agricole (TRA, Quad, MAGA)"),
    TM(9, "Tricycle", "Tricycle à moteur"),
    CYCL(10, "Cyclomoteur 3 roues", "Cyclomoteur carrossé à 3 roues"),
    TCP(11, "Transport commun", "Transport en commun de personnes"),
    TRR(12, "Tracteur routier", "Tracteur routier"),
    REM(13, "Remorque", "Remorque ou semi-remorque"),
    RESP(14, "Réserve spécifique", "Réserve spécifique"),
    REA(15, "Remorque agricole", "Remorque agricole"),
    SREA(16, "Semi-remorque agricole", "Semi-remorque agricole"),
    SRSP(17, "Semi-remorque spécifique", "Usage particulier"),
    SRTC(18, "Semi-remorque tractable", "Semi-remorque spéciale"),
    VU(19, "VU", "Véhicule utilitaire léger"),
    MINIBUS(20, "Minibus", "9 à 16 places assises");

    companion object {
        fun from(code: Int?): GenreVehicle {
            return entries.find { it.code == code } ?: UNKNOW
        }
        fun codeFromLabel(label: String): Int? {
            return (entries.find { it.label == label } ?: UNKNOW).code
        }
    }
}