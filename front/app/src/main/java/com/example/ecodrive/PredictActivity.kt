package com.example.ecodrive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ecodrive.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PredictActivity : AppCompatActivity() {
    // Base de données
    private lateinit var db: AppDatabase

    // UI Components
    private lateinit var usedPriceMin: TextView
    private lateinit var usedPriceMax: TextView
    private lateinit var newPriceMin: TextView
    private lateinit var newPriceMax: TextView
    private lateinit var btnFindNearestGarage: Button
    private lateinit var btnPrepareAd: Button
    private lateinit var btnAddToList: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.predict_activity)

        // Initialiser la base de données
        db = AppDatabase.getDatabase(this)

        // Récupérer les vues
        usedPriceMin = findViewById(R.id.usedPriceMin)
        usedPriceMax = findViewById(R.id.usedPriceMax)
        newPriceMin = findViewById(R.id.newPriceMin)
        newPriceMax = findViewById(R.id.newPriceMax)
        btnFindNearestGarage = findViewById(R.id.btnFindNearestGarage)
        btnPrepareAd = findViewById(R.id.btnPrepareAd)
        btnAddToList = findViewById(R.id.btnAddToList)

        // Récupérer les données de l'intent
        val vehicleData = intent.getSerializableExtra("vehicleData") as? Map<String, String>

        // Afficher les prix estimés (exemple : à remplacer par tes calculs réels)
        usedPriceMin.text = vehicleData?.get("uPmv") ?: " -- "
        usedPriceMax.text = vehicleData?.get("uPMv") ?: " -- "
        newPriceMin.text = vehicleData?.get("nPmv") ?: " -- "
        newPriceMax.text = vehicleData?.get("nPMv") ?: " -- "

        // Bouton "Trouver le garage le plus proche"
        btnFindNearestGarage.setOnClickListener {
            val brand = vehicleData?.get("brand") ?: return@setOnClickListener
            openMapsForNearbyGarages(brand)
        }

        // Bouton "Préparer une annonce"
        btnPrepareAd.setOnClickListener {
            val intent = Intent(this, AdActivity::class.java).apply {
                putExtra("vehicleData", vehicleData as java.io.Serializable)
            }
            startActivity(intent)
        }

        // Bouton "Ajouter à une liste"
        btnAddToList.setOnClickListener {
            showAddToListDialog(vehicleData)
        }
    }

    private fun openMapsForNearbyGarages(brand: String) {
        // Ouvre l'application Maps avec une recherche de garages de la marque
        val query = "$brand garage"
        val gmmIntentUri = Uri.parse("geo:0,0?q=$query")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            // Si Google Maps n'est pas installé, ouvre dans le navigateur
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.openstreetmap.org/search?query=$query")
            )
            startActivity(webIntent)
        }
    }

    private fun showAddToListDialog(vehicleData: Map<String, String>?) {
        // Récupérer les listes existantes
        CoroutineScope(Dispatchers.IO).launch {
            val vehicleLists = db.vehicleListDao().getAll()

            withContext(Dispatchers.Main) {
                val items = vehicleLists.map { it.name }.toTypedArray()
                val dialogItems = items + "Créer une nouvelle liste"

                AlertDialog.Builder(this@PredictActivity)
                    .setTitle("Ajouter à une liste")
                    .setItems(dialogItems) { _, which ->
                        if (which == items.size)
                            // Créer une nouvelle liste
                            showCreateNewListDialog(vehicleData)
                        else
                            // Ajouter à une liste existante
                            addVehicleToList(vehicleLists[which], vehicleData)
                    }
                    .show()
            }
        }
    }

    private fun showCreateNewListDialog(vehicleData: Map<String, String>?) {
        val editText = android.widget.EditText(this).apply {
            hint = "Nom de la nouvelle liste"
        }

        AlertDialog.Builder(this)
            .setTitle("Nouvelle liste")
            .setView(editText)
            .setPositiveButton("Créer") { _, _ ->
                val listName = editText.text.toString()
                if (listName.isNotBlank()) {
                    createNewListAndAddVehicle(listName, vehicleData)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun createNewListAndAddVehicle(listName: String, vehicleData: Map<String, String>?) {
        CoroutineScope(Dispatchers.IO).launch {
            // Créer la nouvelle liste
            val newListId = db.vehicleListDao().insert(VehicleList(name = listName))

            // Récupérer la liste nouvellement créée
            val newList = db.vehicleListDao().getById(newListId.toInt())

            // Ajouter le véhicule à la liste
            newList?.let { addVehicleToList(it, vehicleData) }
        }
    }

    private fun addVehicleToList(vehicleList: VehicleList, vehicleData: Map<String, String>?) {
        CoroutineScope(Dispatchers.IO).launch {
            // Vérifier si le véhicule existe déjà
            val existingVehicle = db.vehicleDao().getByPlateNumber(vehicleData?.get("plateNumber") ?: "")

            val vehicleId = if (existingVehicle != null) {
                // Le véhicule existe déjà
                existingVehicle.id
            } else {
                // Créer un nouveau véhicule
                val newVehicle = Vehicle(
                    brand = vehicleData?.get("brand") ?: "",
                    model = vehicleData?.get("model") ?: "",
                    registrationDate = vehicleData?.get("date") ?: "",
                    color = vehicleData?.get("color") ?: "",
                    energyType = vehicleData?.get("energyType") ?: "",
                    gearboxType = vehicleData?.get("gearboxType") ?: "",
                    gearboxSpeed = vehicleData?.get("gearboxSpeed")?.toIntOrNull() ?: 0,
                    mileage = vehicleData?.get("mileage")?.toIntOrNull() ?: 0,
                    plateNumber = vehicleData?.get("plateNumber") ?: ""
                )
                db.vehicleDao().insert(newVehicle)
            }

            // Vérifier si le véhicule est déjà dans cette liste
            val existingVehicleInList = db.vehicleInListDao().getByVehicleAndList(vehicleId, vehicleList.id)

            if (existingVehicleInList == null) {
                // Ajouter le véhicule à la liste
                db.vehicleInListDao().insert(
                    VehicleInList(
                        vehicleId = vehicleId,
                        listId = vehicleList.id
                    )
                )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@PredictActivity, "Véhicule ajouté à la liste ${vehicleList.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}