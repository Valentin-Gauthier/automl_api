package com.example.ecodrive

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ecodrive.database.AppDatabase
//import com.example.ecodrive.ScanActivity
//import com.example.ecodrive.SearchActivity
//import com.example.ecodrive.VehicleInformationActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
//    private val db: AppDatabase = AppDatabase.getDatabase(this)
    private val gallerySize: Int = 2// Nombre de voitures affiché sur chaque ligne de la gallery//TODO: export to parameter classes
    private lateinit var tabsContainer: LinearLayout
    private lateinit var galleryContainer: ScrollView
    private lateinit var galleryVoid: View
    private var oldSelectedTabId: Int? = -1
    private var isLoading = true // Pour gérer le SplashScreen
//    private val tabsBackgroundColor = TypedValue().apply {
//        theme.resolveAttribute(android.R.attr.colorBackground, this, true)
//    }.data
//    private val tabsBackgroundColorAlt = TypedValue().apply {
//        theme.resolveAttribute(android.R.attr.colorBackground, this, true)
//    }.data

    override fun onCreate(savedInstanceState: Bundle?) {
        // Gérer le SplashScreen
//        val splashScreen = installSplashScreen()
//        splashScreen.setKeepOnScreenCondition { isLoading } // Bloque sur le SplashScreen le temps du chargement
        // Création de l'activité principal
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // Récuperer les éléments de l'activité
//        tabsContainer = findViewById(R.id.tabs)
//        galleryContainer = findViewById(R.id.content)
//        galleryVoid = layoutInflater.inflate(R.layout.gallery_void, galleryContainer)

        // Préparer la vue d'une liste de voiture vide.
//        galleryVoid.findViewById<Button>(R.id.btnAddFirstVehicle).setOnClickListener {
//TODO:(waitdefined)            startActivity(Intent(this, ScanActivity))
//        }

        // Préparer le footer
        // Unactive for MainActivity :
        //findViewById<Button>(R.id.btnHome).setOnClickListener {
        //    startActivity(this)// Return to this activity if this button is clicked
        //}
//        findViewById<Button>(R.id.btnAddVehicle).setOnClickListener {
//TODO:(waitdefined)            startActivity(Intent(this, ScanActivity))
//        }
//        findViewById<Button>(R.id.btnSearch).setOnClickListener {
//TODO:(waitdefined)            startActivity(Intent(this, SearchActivity))
//        }

        // Charger les onglets et le contenu
//        modifyTabs()

        // Fermer le splashScreen et le remplacer par la vue sur l'activité principale
        isLoading = false

        // Préparer opencv dans un thread, pendant que l'utilisateur utilise l'application.
        thread { OpenCVLoader.init() }.start()
    }
/*
    private fun modifyTabs() {
        CoroutineScope(Dispatchers.IO).launch {
            db.vehicleList().getAll().collect { lists ->
                withContext(Dispatchers.Main) {
                    // Oublier les anciens onglets.
                    var tabSize = 0
                    var firstTabId: Int? = null
                    var firstListId: Int? = null
                    tabsContainer.removeAllViews()
                    tabsContainer.visibility = View.GONE
                    // Ajouter les nouveaux onglets.
                    for (list in lists) {
                        val tabId = View.generateViewId() // Génère un ID unique
                        val tabButton = Button(this@MainActivity).apply {
                            id = tabId
                            text = list.name
                            setOnClickListener { changeTabs(tabId, list.id) }
                            setBackgroundColor(getColor(tabsBackgroundColor))
                        }
                        tabsContainer.addView(tabButton)
                        if (tabSize < 1) {
                            firstTabId = tabId
                            firstListId = list.id
                        }
                        tabSize++
                    }
                    // Sélectionner le nouvel onglet actif
                    if (tabSize > 1) {// Plusieurs onglets possibles
                        tabsContainer.visibility = View.VISIBLE
                        oldSelectedTabId?.let {// Mais aucun sélectionné
                            changeTabs(firstTabId, firstListId)// ⇒ Sélection du premier onglet
                        }
                    } else if (tabSize == 1) {// Un seul onglet possible
                        changeTabs(firstTabId, firstListId)// ⇒ Sélection de l'onglet
                    } else {// Suppression du dernier onglet
                        changeTabs(null, null)// ⇒ Aucun onglet sélectionné
                    }
                }
            }
        }
    }

    private fun changeTabs(selectedTabId: Int?, selectedListId: Int?) {
        if (oldSelectedTabId == selectedTabId) return
        galleryContainer.removeAllViews()

        // Désélectionner l'ancien onglet
        oldSelectedTabId?.let {
            findViewById<Button>(oldSelectedTabId!!).setBackgroundColor(getColor(tabsBackgroundColor))// Unselected
            galleryContainer.addView(galleryVoid)// Vider le contenu de la gallery
        }
        oldSelectedTabId = selectedTabId

        // Sélection d'un nouvel onglet
        selectedTabId?.let {
            selectedListId ?: return
            findViewById<Button>(selectedTabId).setBackgroundColor(getColor(tabsBackgroundColorAlt))// Selected

            // Charger les véhicules
            var listSize = 0
            var colCount = 0
            val gallery = TableLayout(this)
            var row = TableRow(this)
            CoroutineScope(Dispatchers.IO).launch {
                db.vehicleInList().getAllFrom(selectedListId).collect { vehicles ->
                    withContext(Dispatchers.Main) {
                        for (vehicle in vehicles) {
                            listSize++
                            if (colCount >= gallerySize) {
                                gallery.addView(row)
                                row = TableRow(this@MainActivity)
                                colCount = 0
                            }
                            // Préparé un nouveau véhicule à ajouter à la gallery.
                            val vehicleItem = layoutInflater.inflate(R.layout.vehicle_item, row, false)
                            vehicleItem.findViewById<TextView>(R.id.vehiclePriceLow).text =
                                minOf(vehicle.priceNewMin, vehicle.priceUsedMin).toString()
                            vehicleItem.findViewById<TextView>(R.id.vehiclePriceHigh).text =
                                maxOf(vehicle.priceNewMax, vehicle.priceUsedMax).toString()
                            vehicleItem.setOnClickListener {
//TODO:(waitdefined)                                val intent = Intent(this@MainActivity, VehicleInformationActivity).apply {
//                                    putExtra("vehicle_id", vehicle.id)
//                                }
//                                startActivity(intent)
                            }
                            // Obtenir la photo du véhicule
                            var imgFile = File(vehicle.pp)
                            if (imgFile.exists()) {
                                val d = Drawable.createFromPath(imgFile.absolutePath)
                                vehicleItem.background = d
                            }
                            // Obtenir le logo de la marque du véhicule
                            db.vehicle().getBrandOf(vehicle.prototypeId).collect { brand ->
                                imgFile = File(brand.pp)
                                if (imgFile.exists()) {
                                    vehicleItem.findViewById<ImageView>(R.id.vehicleBrandLogo)
                                        .setImageURI(imgFile.toUri())
                                }
                            }
                            // Ajouter le véhicule à la gallery
                            row.addView(vehicleItem)//Add cell to table row
                            colCount++
                        }
                        if (colCount > 0) gallery.addView(row)
                        galleryContainer.addView(gallery)
                    }
                }
            }
        }
    }*/
}