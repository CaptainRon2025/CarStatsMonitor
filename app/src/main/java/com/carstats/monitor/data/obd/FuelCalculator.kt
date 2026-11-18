package com.carstats.monitor.data.obd

/**
 * Калькулятор витрати палива та параметрів поїздки (дистанція, середні показники).
 */
class FuelCalculator(
    private val airFuelRatio: Float = 14.7f, // Стехіометричне співвідношення для бензину (14.7:1)
    private val fuelDensityGramsPerLiter: Float = 750f // Густина бензину ~750 г/л
) {
    private var totalFuelUsedLiters: Double = 0.0
    private var totalDistanceKm: Double = 0.0
    private var lastCalculationTimeMs: Long = 0L

    fun reset() {
        totalFuelUsedLiters = 0.0
        totalDistanceKm = 0.0
        lastCalculationTimeMs = 0L
    }

    /**
     * Розраховує миттєву витрату палива (л/год).
     */
    fun calculateLitersPerHour(mafGramsPerSec: Float): Float {
        if (mafGramsPerSec <= 0f) return 0f
        // Формула: (MAF * 3600) / (AFR * Density)
        return (mafGramsPerSec * 3600f) / (airFuelRatio * fuelDensityGramsPerLiter)
    }

    /**
     * Розраховує миттєву витрату на 100 км (л/100км).
     * Якщо швидкість менше 5 км/год, повертає 0 (на холостому ходу показується у л/год).
     */
    fun calculateLitersPer100Km(litersPerHour: Float, speedKmh: Int): Float {
        if (speedKmh < 5 || litersPerHour <= 0f) return 0f
        return (litersPerHour / speedKmh.toFloat()) * 100f
    }

    /**
     * Оновлює накопичувальні показники поїздки з урахуванням часу між замірами.
     */
    fun updateTripMetrics(speedKmh: Int, mafGramsPerSec: Float): TripMetricsResult {
        val now = System.currentTimeMillis()
        if (lastCalculationTimeMs == 0L) {
            lastCalculationTimeMs = now
            return TripMetricsResult(
                instantLitersPerHour = calculateLitersPerHour(mafGramsPerSec),
                instantLitersPer100Km = 0f,
                tripFuelUsedLiters = 0f,
                tripDistanceKm = 0f,
                averageLitersPer100Km = 0f
            )
        }

        val deltaSeconds = (now - lastCalculationTimeMs) / 1000.0
        lastCalculationTimeMs = now

        val instantLh = calculateLitersPerHour(mafGramsPerSec)
        val instantL100 = calculateLitersPer100Km(instantLh, speedKmh)

        // Накопичення дистанції: S = V * t
        val deltaDistanceKm = (speedKmh / 3600.0) * deltaSeconds
        totalDistanceKm += deltaDistanceKm

        // Накопичення витраченого палива: V_палива = (л/год / 3600) * t
        val deltaFuelLiters = (instantLh / 3600.0) * deltaSeconds
        totalFuelUsedLiters += deltaFuelLiters

        val avgConsumption = if (totalDistanceKm > 0.1) {
            ((totalFuelUsedLiters / totalDistanceKm) * 100.0).toFloat()
        } else {
            0f
        }

        return TripMetricsResult(
            instantLitersPerHour = instantLh,
            instantLitersPer100Km = instantL100,
            tripFuelUsedLiters = totalFuelUsedLiters.toFloat(),
            tripDistanceKm = totalDistanceKm.toFloat(),
            averageLitersPer100Km = avgConsumption
        )
    }
}

data class TripMetricsResult(
    val instantLitersPerHour: Float,
    val instantLitersPer100Km: Float,
    val tripFuelUsedLiters: Float,
    val tripDistanceKm: Float,
    val averageLitersPer100Km: Float
)
