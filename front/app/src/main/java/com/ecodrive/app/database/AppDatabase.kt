package com.ecodrive.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ecodrive.app.database.converter.CarBodyConverter
import com.ecodrive.app.database.converter.EnergyConverter
import com.ecodrive.app.database.converter.GearboxConverter
import com.ecodrive.app.database.converter.PlateTypeConverter
import com.ecodrive.app.database.converter.RegistrationSystemConverter
import com.ecodrive.app.database.converter.VehicleTypeConverter
import com.ecodrive.app.database.dao.*
import com.ecodrive.app.database.entities.*

@Database(
    entities = [
        Brand::class,
        VehiclePrototype::class,
        Vehicle::class,
        VehicleAd::class,
        VehicleInList::class,
        VehicleList::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    CarBodyConverter::class,
    EnergyConverter::class,
    GearboxConverter::class,
    VehicleTypeConverter::class,
    PlateTypeConverter::class,
    RegistrationSystemConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brand(): BrandDao
    abstract fun vehiclePrototype(): VehiclePrototypeDao
    abstract fun vehicle(): VehicleDao
    abstract fun vehicleAd(): VehicleAdDao
    abstract fun vehicleInList(): VehicleInListDao
    abstract fun vehicleList(): VehicleListDao

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
