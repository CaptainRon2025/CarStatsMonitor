package com.carstats.monitor.domain.model

/**
 * Код діагностичної помилки двигуна (DTC).
 */
data class DtcCode(
    val code: String,          // наприклад, "P0133"
    val description: String,   // наприклад, "02 Sensor Circuit Slow Response (Bank 1 Sensor 1)"
    val category: String = "Двигун / Трансмісія",
    val severity: DtcSeverity = DtcSeverity.WARNING
)

enum class DtcSeverity {
    INFO,
    WARNING,
    CRITICAL
}
