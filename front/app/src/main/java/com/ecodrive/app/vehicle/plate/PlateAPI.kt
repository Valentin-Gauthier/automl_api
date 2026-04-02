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
        plate: PlateInformation,
        onSuccess: (VehicleInformation) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val number = plate.number
                when (plate.registrationSystem) {
                    RegistrationSystem.SIV -> {
                        val info = callSivApi(number)
                        if (info != null) onSuccess(info)
                        else onError("Véhicule non trouvé pour la plaque $number")
                    }
                    RegistrationSystem.FNI -> {
                        // Aucune API FNI publique disponible
                        onError("Recherche FNI non supportée pour la plaque $number")
                    }
                    else -> {
                        onError("Système d'immatriculation inconnu.")
                        return@execute
                    }
                }
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
        val url = URL(
            "https://api.apiplaqueimmatriculation.com/plaque" +
                "?immatriculation=$formattedPlate" +
                "&token=$apiToken" +
                "&pays=FR"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod  = "POST"
            connectTimeout = 10_000
            readTimeout    = 10_000
            setRequestProperty("Accept", "application/json")
        }

        val code = conn.responseCode
        if (code != 200) throw Exception("HTTP $code")

        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val json = JSONObject(body)
        // L'API renvoie { "success" : false } si non trouvé
        if (!json.optBoolean("success", true)) return null

        // Certaines APIs encapsulent dans "data", d'autres non
        val data = if (json.has("data")) json.getJSONObject("data") else json

        return VehicleInformation(
            // IDENTIFICATION
            data.str("vin") ?: "",
            // ORIGIN
            data.str("pays") ?: "FR",
            // BRAND
            data.str("marque")      ?: "",
            data.str("logo_marque") ?: "",
            // MODEL
            data.str("modele")       ?: "",
            data.str("version")      ?: "",
            data.str("debut_modele") ?: "",
            data.str("fin_modele")   ?: "",

            data.int("genreVCG")         ?: 0,
            data.int("code_carrosserie") ?: 0,
            // POWERTRAIN
            data.str("boite_vitesse")  ?: "",
            data.int("puisFisc")       ?: 0,
            data.int("energie")        ?: 0,
            data.int("puisFiscReelKW") ?: 0,
            data.int("puisFiscReelCH") ?: 0,
            // APPEARANCE
            data.int("nr_passagers") ?: 0,
            data.int("nb_portes")    ?: 0,
            data.str("couleur")      ?: "",
            // DIMENSIONS & WEIGHT
            data.int("poids")   ?: 0,
            data.int("ptac")    ?: 0,
            data.int("longueur")?: 0,
            data.int("largeur") ?: 0,
            data.int("hauteur") ?: 0,
            data.int("empattement") ?: 0,
            // REGISTRATION & USAGE
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
