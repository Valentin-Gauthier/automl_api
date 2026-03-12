package com.example.ecodrive

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ecodrive.database.AppDatabase
import com.example.ecodrive.database.VehicleDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser la base de données
        val db = AppDatabase.getDatabase(this)
        val vehicleDao = db.vehicleDao()

        // Vérifier si des véhicules sont enregistrés
        CoroutineScope(Dispatchers.IO).launch {
            val vehicleCount = vehicleDao.getVehicleCount()

            withContext(Dispatchers.Main) {
                if (vehicleCount > 0) {
                    // Lancer GalleryActivity
                    startActivity(Intent(this@SplashActivity, GalleryActivity::class.java))
                } else {
                    // Lancer MainAltActivity
                    startActivity(Intent(this@SplashActivity, MainAltActivity::class.java))
                }
                finish() // Terminer SplashActivity
            }
        }
    }
}