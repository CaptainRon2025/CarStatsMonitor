package com.carstats.monitor

import android.app.Application
import com.carstats.monitor.data.bluetooth.BluetoothSerialManager
import com.carstats.monitor.data.db.CarDatabase
import com.carstats.monitor.data.obd.ObdStreamEngine

/**
 * Головний клас Application для ініціалізації залежностей бази даних та Bluetooth.
 */
class CarStatsApplication : Application() {

    lateinit var database: CarDatabase
        private set

    lateinit var bluetoothManager: BluetoothSerialManager
        private set

    lateinit var obdStreamEngine: ObdStreamEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = CarDatabase.getDatabase(this)
        bluetoothManager = BluetoothSerialManager(this)
        obdStreamEngine = ObdStreamEngine(bluetoothManager)
    }
}
