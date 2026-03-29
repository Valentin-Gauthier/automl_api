package com.ecodrive.app.vehicle

enum class GearboxEnergy(
    override val code: Int?,
    override val label: String,
    override val description: String
) : SpinnerItem<Int> {
    UNKNOW(null, "Inconnu", "Énergie non reconnue"),

    BIOETHANOL(1, "Bioéthanol", "Carburant E85"),
    DIESEL(2, "Diesel", "Gazole"),
    ESS_GPL(3, "Essence + GPL", "Bicarburation"),
    ESSENCE(4, "Essence", "Carburant essence"),
    HYBRIDE_ESS(5, "Hybride essence", "Essence + électrique"),
    HYBRIDE_RECH_ESS(6, "Hybride rechargeable essence", "Plug-in essence"),
    FLEXFUEL_HYB(7, "Flexfuel hybride", "Multi-carburants hybride"),
    GAZ_NATUREL(8, "Gaz naturel", "GNV"),
    HYBRIDE_DIESEL(9, "Hybride diesel", "Diesel + électrique"),
    HYBRIDE_RECH_DIESEL(10, "Hybride rechargeable diesel", "Plug-in diesel"),
    GPL(11, "GPL", "Gaz de pétrole liquéfié"),
    ELECTRIQUE(12, "Électrique", "100% électrique"),
    ESS_ETHANOL(13, "Essence / Éthanol", "Mix carburants"),
    ESS_ELEC(14, "Essence / Électrique", "Hybride simple"),
    DIESEL_ELEC(15, "Diesel / Électrique", "Hybride diesel"),
    ESS_GAZ(16, "Essence / Gaz", "Mix gaz naturel"),
    ELEC_ETHANOL(17, "Élec / Éthanol", "Hybride spécifique"),
    HYBRIDE_RECH(18, "Hybride rechargeable", "Essence + élec plug-in"),
    HYDROGENE(19, "Hydrogène", "Pile à combustible");

    companion object {
        fun from(code: Int?): GearboxEnergy {
            return entries.find { it.code == code } ?: UNKNOW
        }
        fun codeFromLabel(label: String): Int? {
            return (entries.find { it.label == label } ?: UNKNOW).code
        }
    }
}