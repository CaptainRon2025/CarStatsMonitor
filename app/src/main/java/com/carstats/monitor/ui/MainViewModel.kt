package com.carstats.monitor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carstats.monitor.CarStatsApplication
import com.carstats.monitor.data.bluetooth.BluetoothDeviceModel
import com.carstats.monitor.data.db.TripEntity
import com.carstats.monitor.data.db.TripLogPointEntity
import com.carstats.monitor.domain.model.CarTelemetry
import com.carstats.monitor.domain.model.ConnectionStatus
import com.carstats.monitor.domain.model.DtcCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CarStatsApplication
    private val bluetoothManager = app.bluetoothManager
    private val obdEngine = app.obdStreamEngine
    private val tripDao = app.database.tripDao()

    val connectionStatus: StateFlow<ConnectionStatus> = obdEngine.connectionStatus
    val telemetry: StateFlow<CarTelemetry> = obdEngine.telemetry

    // Список спарених пристроїв Bluetooth
    private val _bondedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDeviceModel>> = _bondedDevices.asStateFlow()

    // Історичні буфери для відображення графіків (останні 30 значень)
    private val _speedHistory = MutableStateFlow<List<Float>>(emptyList())
    val speedHistory: StateFlow<List<Float>> = _speedHistory.asStateFlow()

    private val _rpmHistory = MutableStateFlow<List<Float>>(emptyList())
    val rpmHistory: StateFlow<List<Float>> = _rpmHistory.asStateFlow()

    private val _coolantHistory = MutableStateFlow<List<Float>>(emptyList())
    val coolantHistory: StateFlow<List<Float>> = _coolantHistory.asStateFlow()

    private val _fuelRateHistory = MutableStateFlow<List<Float>>(emptyList())
    val fuelRateHistory: StateFlow<List<Float>> = _fuelRateHistory.asStateFlow()

    // Журнал поїздок з Room DB
    val trips: StateFlow<List<TripEntity>> = tripDao.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Стан поточної активної поїздки
    private val _currentTripId = MutableStateFlow<Long?>(null)
    val isTripActive: StateFlow<Boolean> = MutableStateFlow(false)

    // Діагностичні коди помилок (DTC)
    private val _dtcCodes = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcCodes: StateFlow<List<DtcCode>> = _dtcCodes.asStateFlow()

    private val _isDtcScanning = MutableStateFlow(false)
    val isDtcScanning: StateFlow<Boolean> = _isDtcScanning.asStateFlow()

    private val _dtcStatusMessage = MutableStateFlow<String?>(null)
    val dtcStatusMessage: StateFlow<String?> = _dtcStatusMessage.asStateFlow()

    private var pollingJob: Job? = null
    private var historyCollectorJob: Job? = null
    private var tripLoggerJob: Job? = null

    init {
        loadBondedDevices()
        startHistoryCollector()
    }

    fun loadBondedDevices() {
        _bondedDevices.value = bluetoothManager.getBondedDevices()
    }

    /**
     * Повний цикл підключення: сокет -> ініціалізація ELM327 -> запуск циклу опитування.
     */
    fun connectToDevice(device: BluetoothDeviceModel) {
        viewModelScope.launch {
            _connectionStatusError(null)
            obdEngine.stopPolling()
            pollingJob?.cancel()

            val connectResult = bluetoothManager.connect(device.address)
            if (connectResult.isFailure) {
                return@launch
            }

            val initialized = obdEngine.initializeAdapter(device.name)
            if (initialized) {
                startPolling()
            }
        }
    }

    private fun _connectionStatusError(msg: String?) {
        // internal helper
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            obdEngine.startPollingLoop()
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        obdEngine.stopPolling()
        bluetoothManager.disconnect()
        if (_currentTripId.value != null) {
            finishTrip()
        }
    }

    /**
     * Буферизація останніх точок для побудови графіків.
     */
    private fun startHistoryCollector() {
        historyCollectorJob?.cancel()
        historyCollectorJob = viewModelScope.launch {
            telemetry.collect { t ->
                _speedHistory.value = (_speedHistory.value + t.speedKmh.toFloat()).takeLast(30)
                _rpmHistory.value = (_rpmHistory.value + t.engineRpm.toFloat()).takeLast(30)
                _coolantHistory.value = (_coolantHistory.value + t.coolantTempC.toFloat()).takeLast(30)
                _fuelRateHistory.value = (_fuelRateHistory.value + t.instantaneousFuelFlowLitersPerHour).takeLast(30)
            }
        }
    }

    // --- Керування поїздкою (Trip Logger) ---

    fun startTrip(title: String = "Поїздка") {
        viewModelScope.launch {
            val trip = TripEntity(
                startTime = System.currentTimeMillis(),
                title = title
            )
            val id = tripDao.insertTrip(trip)
            _currentTripId.value = id

            // Запускаємо періодичний запис точок телеметрії (кожні 3 секунди)
            tripLoggerJob?.cancel()
            tripLoggerJob = viewModelScope.launch {
                while (_currentTripId.value == id) {
                    delay(3000)
                    val current = telemetry.value
                    tripDao.insertLogPoint(
                        TripLogPointEntity(
                            tripId = id,
                            timestamp = System.currentTimeMillis(),
                            speedKmh = current.speedKmh,
                            rpm = current.engineRpm,
                            coolantTempC = current.coolantTempC,
                            fuelFlowLh = current.instantaneousFuelFlowLitersPerHour,
                            voltage = current.batteryVoltage
                        )
                    )
                }
            }
        }
    }

    fun finishTrip() {
        val tripId = _currentTripId.value ?: return
        viewModelScope.launch {
            tripLoggerJob?.cancel()
            val current = telemetry.value
            val existingTrip = tripDao.getTripById(tripId)
            if (existingTrip != null) {
                val duration = (System.currentTimeMillis() - existingTrip.startTime) / 1000
                val updated = existingTrip.copy(
                    endTime = System.currentTimeMillis(),
                    durationSeconds = duration,
                    distanceKm = current.tripDistanceKm,
                    fuelUsedLiters = current.tripFuelUsedLiters,
                    avgConsumptionL100km = current.averageFuelConsumptionL100km
                )
                tripDao.updateTrip(updated)
            }
            _currentTripId.value = null
        }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            tripDao.deleteTrip(trip)
        }
    }

    // --- Діагностика помилок (DTC) ---

    fun readTroubleCodes() {
        viewModelScope.launch {
            _isDtcScanning.value = true
            _dtcStatusMessage.value = "Зчитування кодів помилок з ЕБУ…"
            val codes = obdEngine.readDiagnosticCodes()
            _dtcCodes.value = codes
            _isDtcScanning.value = false
            _dtcStatusMessage.value = if (codes.isEmpty()) {
                "Помилок не виявлено (DTC відсутні) ✅"
            } else {
                "Знайдено помилок: ${codes.size}"
            }
            // Відновлюємо потік після перевірки
            if (bluetoothManager.isConnected) {
                startPolling()
            }
        }
    }

    fun clearTroubleCodes() {
        viewModelScope.launch {
            _isDtcScanning.value = true
            _dtcStatusMessage.value = "Надсилання команди скидання помилок (Mode 04)…"
            val success = obdEngine.clearDiagnosticCodes()
            _isDtcScanning.value = false
            if (success) {
                _dtcCodes.value = emptyList()
                _dtcStatusMessage.value = "Помилки успішно скинуто! Check Engine скинуто."
            } else {
                _dtcStatusMessage.value = "Не вдалося скинути помилки. Перевірте положення запалювання (двигун має бути заглушений, запалювання увімкнене)."
            }
            if (bluetoothManager.isConnected) {
                startPolling()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
