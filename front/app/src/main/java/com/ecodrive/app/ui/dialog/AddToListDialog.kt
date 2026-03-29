package com.ecodrive.app.ui.dialog

import android.content.Context
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.ecodrive.app.R
import com.ecodrive.app.database.AppDatabase
import com.ecodrive.app.database.entities.Vehicle
import com.ecodrive.app.database.entities.VehicleInList
import com.ecodrive.app.database.entities.VehicleList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog permettant de sélectionner une ou plusieurs listes
 * et d'y ajouter un véhicule, ou de créer une nouvelle liste.
 */
class AddToListDialog(
    private val context: Context,
    private val vehicle: Vehicle,
    private val onDone: () -> Unit = {}
) {
    private val db = AppDatabase.getDatabase(context)

    fun show() {
        CoroutineScope(Dispatchers.IO).launch {
            val lists = db.vehicleList().getAll().first()
            withContext(Dispatchers.Main) {
                buildAndShow(lists)
            }
        }
    }

    private fun buildAndShow(lists: List<VehicleList>) {
        val checkedStates = BooleanArray(lists.size) { false }
        val dp8 = (8 * context.resources.displayMetrics.density).toInt()
        val dp16 = dp8 * 2

        // Conteneur principal
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16, dp16, dp16, dp8)
        }

        // Listes existantes avec cases à cocher
        if (lists.isNotEmpty()) {
            root.addView(TextView(context).apply {
                text = context.getString(R.string.dialog_select_lists)
                textSize = 14f
                setPadding(0, 0, 0, dp8)
            })

            val scrollView = ScrollView(context)
            val checkContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            lists.forEachIndexed { i, list ->
                val cb = CheckBox(context).apply {
                    text = list.name
                    setPadding(0, dp8, 0, dp8)
                    setOnCheckedChangeListener { _, checked -> checkedStates[i] = checked }
                }
                checkContainer.addView(cb)
            }
            scrollView.addView(checkContainer)
            root.addView(scrollView)
        } else {
            root.addView(TextView(context).apply {
                text = context.getString(R.string.dialog_no_list_yet)
                textSize = 14f
                setPadding(0, 0, 0, dp8)
            })
        }

        // Nouvelle liste
        root.addView(TextView(context).apply {
            text = context.getString(R.string.dialog_create_new_list)
            textSize = 14f
            setPadding(0, dp16, 0, dp8)
        })
        val newListInput = EditText(context).apply {
            hint = context.getString(R.string.dialog_new_list_name_hint)
        }
        root.addView(newListInput)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_add_to_list_title))
            .setView(root)
            .setPositiveButton(context.getString(R.string.dialog_confirm)) { _, _ ->
                onConfirm(lists, checkedStates, newListInput.text.toString().trim())
            }
            .setNegativeButton(context.getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun onConfirm(lists: List<VehicleList>, checked: BooleanArray, newListName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (newListName.isNotBlank()) {
                    val newId = db.vehicleList().insert(VehicleList(name = newListName))
                    db.vehicleInList().insert(VehicleInList(vehicle.immatriculation, newId.toInt()))
                }
                lists.forEachIndexed { i, list ->
                    if (checked[i]) {
                        try {
                            db.vehicleInList().insert(
                                VehicleInList(
                                    vehicle.immatriculation,
                                    list.id
                                )
                            )
                        } catch (_: Exception) { /* Déjà présent dans cette liste */ }
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.dialog_vehicle_added), Toast.LENGTH_SHORT).show()
                    onDone()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
