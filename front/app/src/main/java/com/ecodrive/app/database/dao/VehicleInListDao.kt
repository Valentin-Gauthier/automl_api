package com.ecodrive.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ecodrive.app.database.entities.Vehicle
import com.ecodrive.app.database.entities.VehicleInList
import com.ecodrive.app.database.entities.VehicleList

@Dao
interface VehicleInListDao {

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    @Insert
    suspend fun insertAll(links: List<VehicleInList>)

    suspend fun insertAll(vehicleId: String, listIds: List<Int>) =
        insertAll(listIds.map { VehicleInList(vehicleId, it) })

    suspend fun insertAll(vehicle: Vehicle, listIds: List<Int>) =
        insertAll(vehicle.id, listIds)

    suspend fun insertAll(vehicleId: String, vehicleList: List<VehicleList>) =
        insertAll(vehicleList.map { VehicleInList(vehicleId, it.id) })

    suspend fun insertAll(vehicle: Vehicle, vehicleList: List<VehicleList>) =
        insertAll(vehicleList.map { VehicleInList(vehicle.id, it.id) })

    @Query("""
        DELETE FROM Vehicles_in_Lists
        WHERE VehicleId = :vehicleId
          AND ListId IN (:listIds)
    """)
    suspend fun deleteAll(vehicleId: String, listIds: List<Int>)

    suspend fun deleteAll(vehicle: Vehicle, listIds: List<Int>) =
        deleteAll(vehicle.id, listIds)

    suspend fun deleteAll(vehicleId: String, vehicleList: List<VehicleList>) =
        deleteAll(vehicleId, vehicleList.map { it.id })

    suspend fun deleteAll(vehicle: Vehicle, vehicleList: List<VehicleList>) =
        deleteAll(vehicle.id, vehicleList.map { it.id })

    @Query("DELETE FROM Vehicles_in_Lists WHERE VehicleId = :vehicleId")
    suspend fun deleteAll(vehicleId: String)

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @Query("""
        SELECT ListId FROM Vehicles_in_Lists
        WHERE VehicleId = :id
    """)
    suspend fun getListIdsForVehicle(id: String): List<Int>

    suspend fun getListIdsForVehicle(v: Vehicle): List<Int> =
        getListIdsForVehicle(v.id)
}
