package com.carstats.monitor.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Точка логу телеметрії (зберігається з періодичністю під час поїздки для побудови графіків та експорту в CSV).
 */
@Entity(
    tableName = "trip_log_points",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"])]
)
data class TripLogPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val timestamp: Long,
    val speedKmh: Int,
    val rpm: Int,
    val coolantTempC: Int,
    val fuelFlowLh: Float,
    val voltage: Float
)
