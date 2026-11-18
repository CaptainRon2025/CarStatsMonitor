package com.carstats.monitor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Запис про поїздку в локальній базі даних.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val distanceKm: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Int = 0,
    val fuelUsedLiters: Float = 0f,
    val avgConsumptionL100km: Float = 0f,
    val durationSeconds: Long = 0L,
    val title: String = "Поїздка"
)
