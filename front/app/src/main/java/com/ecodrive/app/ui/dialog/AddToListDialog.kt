package com.ecodrive.app.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.ecodrive.app.R
import com.ecodrive.app.database.AppDatabase
import com.ecodrive.app.database.entities.Vehicle
import com.ecodrive.app.database.entities.VehicleList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog permettant de sélectionner une ou plusieurs listes
 * et d'y ajouter un véhicule, ou de créer une nouvelle liste.
 */
class AddToListDialog(
    context: Context,
    private val vehicle: Vehicle,
    private val onDone: () -> Unit = {},
    private val onAbort: () -> Unit = {}
) : Dialog(context) {

    // -------------------------------------------------------------------------
    // Propriétés
    // -------------------------------------------------------------------------

    // Composant tiré du layout installé.
    private lateinit var categoriesContainer: LinearLayout
    private lateinit var newCategory: EditText
    private lateinit var newCategoryButton: Button
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    // Suivi des catégories
    private val db = AppDatabase.getDatabase(context)
    private val checkedCategories = mutableMapOf<Int, Boolean>()
    private var inRun = false

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_categorize)

        // Initialiser les vues
        categoriesContainer=findViewById(R.id.categories)
        newCategory       = findViewById(R.id.category)
        newCategoryButton = findViewById(R.id.categoryBtn)
        confirmButton     = findViewById(R.id.ok_button)
        cancelButton      = findViewById(R.id.cancel_button)

        // Configurer les boutons
        newCategoryButton.setOnClickListener { addNewCategory() }
        confirmButton.setOnClickListener { saveChanges() }
        cancelButton.setOnClickListener { dismiss() }

        // Charger les catégories
        loadCategories()
        inRun = true
    }

    // -------------------------------------------------------------------------
    // Chargement initial
    // -------------------------------------------------------------------------

    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Charger les catégories déjà sélectionnées
                db.vehicleInList()
                    .getListIdsForVehicle(vehicle)
                    .forEach { checkedCategories[it] = true }

                // Observer les changements dans les catégories
                db.vehicleList().getAll().collect { categories ->
                    withContext(Dispatchers.Main) { refresh(categories) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Rafraîchissement de l'UI
    // -------------------------------------------------------------------------

    private fun refresh(categories: List<VehicleList>) {
        categoriesContainer.removeAllViews()
        categories.forEach { category ->
            // Initialise l'état pour les nouvelles listes ajoutées dynamiquement
            if (category.id !in checkedCategories)
                checkedCategories[category.id] = inRun
            // Ajout de la case à cocher reflétant l'état de la liste
            val checkBox = CheckBox(context).apply {
                text = category.name
                isChecked = checkedCategories[category.id] ?: inRun
                setOnCheckedChangeListener { _, checked ->
                    checkedCategories[category.id] = checked
                }
            }
            categoriesContainer.addView(checkBox)
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private fun addNewCategory() {
        val name = newCategory.text.toString().trim().also { newCategory.text.clear() }
        if (name.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val newId = db.vehicleList()
                .insert(VehicleList(name = name))
                .toInt()
            checkedCategories[newId] = true
            // Le Flow de getAll() déclenchera automatiquement refresh()
        }
    }

    private fun saveChanges() {
        CoroutineScope(Dispatchers.IO).launch {
            val alreadySelected = try {
                // Charger les catégories déjà sélectionnées
                 db.vehicleInList().getListIdsForVehicle(vehicle)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val toAdd: List<Int> = checkedCategories
                .filter { (id, state) -> state && id !in alreadySelected }
                .keys
                .toList()

            val toRem: List<Int> = checkedCategories
                .filter { (id, state) -> !state && id in alreadySelected }
                .keys
                .toList()

            // Modification de la base de données
            db.vehicleInList().deleteAll(vehicle.id, toRem)
            db.vehicleInList().insertAll(vehicle.id, toAdd)

            withContext(Dispatchers.Main) {
                onDone.invoke()
                super.dismiss()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Death
    // -------------------------------------------------------------------------

    override fun dismiss() {
        // Abandonner la modification des catégories du véhicule
        onAbort.invoke()
        super.dismiss()
    }
}
