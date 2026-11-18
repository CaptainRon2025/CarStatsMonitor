package com.carstats.monitor.domain.model

/**
 * Стан підключення до Bluetooth ELM327 адаптера.
 */
sealed class ConnectionStatus {
    data object Disconnected : ConnectionStatus()
    data class Scanning(val message: String = "Пошук пристроїв…") : ConnectionStatus()
    data class Connecting(val deviceName: String) : ConnectionStatus()
    data class InitializingElm(val step: String) : ConnectionStatus()
    data class Connected(val deviceName: String, val protocol: String = "Auto (OBD-II)") : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
