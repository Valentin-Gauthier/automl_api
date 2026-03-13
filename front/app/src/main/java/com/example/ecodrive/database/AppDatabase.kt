package com.example.ecodrive.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.example.ecodrive.database.dao.*
import com.example.ecodrive.database.entities.*

@Database(
    entities = [
        Energy::class,
        Gearbox::class,
        GearboxEnergyCompatibility::class,
        Marque::class,
        Vehicle::class,
        VehicleAd::class,
        VehicleInList::class,
        VehicleList::class,
        VehiclePrototype::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun energyDao(): EnergyDao
    abstract fun gearboxDao(): GearboxDao
    abstract fun gearboxEnergyCompatibilityDao(): GearboxEnergyCompatibilityDao
    abstract fun marqueDao(): MarqueDao
    abstract fun vehicleAdDao(): VehicleAdDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun vehicleInListDao(): VehicleInListDao
    abstract fun vehicleListDao(): VehicleListDao
    abstract fun vehiclePrototypeDao(): VehiclePrototypeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}