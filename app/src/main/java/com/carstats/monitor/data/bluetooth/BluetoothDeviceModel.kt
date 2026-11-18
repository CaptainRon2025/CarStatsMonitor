package com.carstats.monitor.data.bluetooth

/**
 * Модель Bluetooth пристрою для відображення у списку.
 */
data class BluetoothDeviceModel(
    val name: String,
    val address: String,
    val isBonded: Boolean = true
)
