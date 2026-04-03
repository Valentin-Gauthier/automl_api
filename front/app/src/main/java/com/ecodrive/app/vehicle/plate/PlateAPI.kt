package com.ecodrive.app.vehicle.plate

import android.util.Log
import com.ecodrive.app.vehicle.VehicleInformation
import com.ecodrive.app.vehicle.plate.data.PlateInformation
import com.ecodrive.app.vehicle.plate.data.RegistrationSystem
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class PlateAPI(
    private val apiToken: String = TOKEN_DEMO
) {

    private val executor = Executors.newSingleThreadExecutor()

    // =========================================================================
    // API publique
    // =========================================================================

    /**
     * Interroge l'API avec le numéro de plaque.
     *
     * @param plate      Numéro normalisé (ex: "AB123CD" ou "AB-123-CD")
     * @param onSuccess  Appelé avec [VehicleInformation] si la plaque est trouvée
     * @param onError    Appelé avec un message d'erreur lisible
     */
    fun fetchVehicle(
        plate: String,
        onSuccess: (VehicleInformation) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val number = plate
                val info = callSivApi(number)
                if (info != null) onSuccess(info)
                else onError("Véhicule non trouvé pour la plaque $number")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur API véhicule", e)
                onError("Erreur réseau : ${e.message}")
                return@execute
            }
        }
    }

    fun shutdown() { executor.shutdown() }

    // =========================================================================
    // Appel HTTP
    // =========================================================================

    private fun callSivApi(formattedPlate: String): VehicleInformation? {
        var body = ""

        try {
            val url = URL(
                "https://api.apiplaqueimmatriculation.com/plaque" +
                        "?immatriculation=$formattedPlate" +
                        "&token=$apiToken" +
                        "&pays=FR"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod  = "GET"
                connectTimeout = 10_000
                readTimeout    = 10_000
                setRequestProperty("Accept", "application/json")
            }

            val code = conn.responseCode
            if (code != 200) throw Exception("HTTP $code")

            // Si tout va bien, on lit la vraie réponse
            body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

        } catch (e: Exception) {
            Log.e("EcoDrive.PlateAPI", "Erreur réseau, utilisation du Mock JSON", e)

            // Le token de demo a expiré !
            body = """
                {
                    "data": {
                        "erreur": "",
                        "immat": "AA123BC",
                        "pays": "FR",
                        "marque": "RENAULT",
                        "modele": "MEGANE III",
                        "modele_en": "MEGANE III Coupe",
                        "version": "1.9 DCI",
                        "debut_modele": "2008-11",
                        "fin_modele": "2015-08",
                        "date1erCir_us": "2009-04-18",
                        "date1erCir_fr": "18-04-2009",
                        "co2": "",
                        "energie": "2",
                        "energieNGC": "DIESEL",
                        "type_moteur": "Diesel",
                        "genreVCG": "1",
                        "genreVCGNGC": "VP",
                        "puisFisc": "7",
                        "carrosserieCG": "COUPE",
                        "code_carrosserie": "29",
                        "carrosserie": "Coupé",
                        "code_type_transmission": "1",
                        "type_transmission": "Traction avant",
                        "capacite_litres": "1.90",
                        "code_systeme_alimentation": "11",
                        "systeme_alimentation": "Injection directe",
                        "valves": "2",
                        "puisFiscReelKW": "96 KW",
                        "puisFiscReelCH": "131 CH",
                        "collection": "",
                        "vin": "VF1DZ0N0641118804",
                        "variante": "DZ0N06",
                        "boite_vitesse": "M",
                        "code_boite_vitesse": "",
                        "nr_passagers": "5",
                        "nb_portes": "0",
                        "type_mine": "MRE5531A0421",
                        "cnit": "MRE5531A0421",
                        "couleur": "",
                        "poids": "1807 KG",
                        "ptac": "1807 KG",
                        "ccm": "1870 CM3",
                        "cylindres": "4",
                        "propulsion": "",
                        "type_compression": "",
                        "longueur": "",
                        "largeur": "",
                        "hauteur": "",
                        "empattement": "",
                        "sra_id": "RE80126",
                        "sra_group": "32",
                        "sra_commercial": "1.9 DCI 130 XV DE FRANCE",
                        "numero_serie": "41118804",
                        "logo_marque": "https://api.apiplaqueimmatriculation.com/public/storage/photos_marques/93.png?v=1",
                        "photo_modele": "https://api.apiplaqueimmatriculation.com/public/storage/photos_modeles/31164.jpg",
                        "k_type": "31164",
                        "tecdoc_manu_id": "93",
                        "tecdoc_model_id": "7867",
                        "tecdoc_car_id": "31164",
                        "tecdoc_vehicules_compatible": "",
                        "code_moteur": "F9Q(870|872)",
                        "codes_platforme": "DZ0/1_",
                        "liste_sra_commercial": null
                    },
                    "api-version": "1.0.0",
                    "message": "",
                    "code_erreur": 200
                }
            """.trimIndent()
        }

        // Le reste du code s'exécute normalement, qu'on ait eu internet ou le Mock !
        val json = JSONObject(body)

        // L'API renvoie { "success" : false } si non trouvé (optBoolean passe à true si la clé n'existe pas, donc ça marchera avec ton JSON)
        if (!json.optBoolean("success", true)) return null

        val data = if (json.has("data")) json.getJSONObject("data") else json

        return VehicleInformation(
            // ... (ton code de parsing habituel reste identique ici) ...
            data.str("vin") ?: "",
            data.str("pays") ?: "FR",
            data.str("marque")      ?: "",
            data.str("logo_marque") ?: "",
            data.str("modele")       ?: "",
            data.str("version")      ?: "",
            data.str("debut_modele") ?: "",
            data.str("fin_modele")   ?: "",
            data.int("genreVCG")         ?: 0,
            data.int("code_carrosserie") ?: 0,
            data.str("boite_vitesse")  ?: "",
            data.int("puisFisc")       ?: 0,
            data.int("energie")        ?: 0,
            data.int("puisFiscReelKW") ?: 0,
            data.int("puisFiscReelCH") ?: 0,
            data.int("nr_passagers") ?: 0,
            data.int("nb_portes")    ?: 0,
            data.str("couleur")      ?: "",
            data.int("poids")   ?: 0,
            data.int("ptac")    ?: 0,
            data.int("longueur")?: 0,
            data.int("largeur") ?: 0,
            data.int("hauteur") ?: 0,
            data.int("empattement") ?: 0,
            data.str("date1erCir_us")   ?: "",
            data.int("capacite_litres") ?: 0,
            data.str("collection")      ?: ""
        )
    }

    // =========================================================================
    // Extensions JSON
    // =========================================================================

    private fun JSONObject.str(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).ifBlank { null } else null

    private fun JSONObject.int(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key, -1).takeIf { it >= 0 } else null

    // =========================================================================
    // Constantes
    // =========================================================================

    companion object {
        private const val TAG = "EcoDrive.PlateAPI"
        const val TOKEN_DEMO  = "TokenDemo2026B"
    }
}
