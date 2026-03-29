package com.ecodrive.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.ecodrive.app.database.dao.*
import com.ecodrive.app.database.entities.*

@Database(
    entities = [
        Energy::class,
        Gearbox::class,
        GearboxEnergyCompatibility::class,
        Brand::class,
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
    abstract fun energy(): EnergyDao
    abstract fun gearbox(): GearboxDao
    abstract fun gearboxEnergyCompatibility(): GearboxEnergyCompatibilityDao
    abstract fun marque(): BrandDao
    abstract fun vehicleAd(): VehicleAdDao
    abstract fun vehicle(): VehicleDao
    abstract fun vehicleInList(): VehicleInListDao
    abstract fun vehicleList(): VehicleListDao
    abstract fun vehiclePrototype(): VehiclePrototypeDao

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