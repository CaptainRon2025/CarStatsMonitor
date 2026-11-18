package com.carstats.monitor.domain.model

/**
 * Поточні телеметричні параметри автомобіля в реальному часі.
 */
data class CarTelemetry(
    val speedKmh: Int = 0,                       // PID 010D (км/год)
    val engineRpm: Int = 0,                      // PID 010C (об/хв)
    val coolantTempC: Int = 0,                   // PID 0105 (°C)
    val engineLoadPercent: Float = 0f,           // PID 0104 (%)
    val throttlePositionPercent: Float = 0f,     // PID 0111 (%)
    val batteryVoltage: Float = 12.6f,           // ATRV (Вольт)
    val mafAirFlowGramsPerSec: Float = 0f,       // PID 0110 (г/с)
    val fuelLevelPercent: Float = 0f,            // PID 012F (%)
    val intakeAirTempC: Int = 0,                 // PID 010F (°C)

    // Розрахункові показники витрати палива
    val instantaneousFuelFlowLitersPerHour: Float = 0f, // л/год (на холостому або під час руху)
    val instantaneousFuelConsumptionL100km: Float = 0f, // л/100км (під час руху)
    val averageFuelConsumptionL100km: Float = 0f,       // Середня витрата за поточну поїздку
    val tripFuelUsedLiters: Float = 0f,                 // Використано палива за поїздку (л)
    val tripDistanceKm: Float = 0f,                     // Пройдена дистанція за поїздку (км)

    val timestamp: Long = System.currentTimeMillis()
)
