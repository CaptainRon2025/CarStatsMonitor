package com.carstats.monitor.data.obd

import com.carstats.monitor.data.bluetooth.BluetoothSerialManager
import com.carstats.monitor.domain.model.CarTelemetry
import com.carstats.monitor.domain.model.ConnectionStatus
import com.carstats.monitor.domain.model.DtcCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Рушій взаємодії з ELM327: ініціалізація адаптера, періодичний опрос PID та оновлення телеметрії.
 */
class ObdStreamEngine(
    private val bluetoothManager: BluetoothSerialManager
) {
    private val fuelCalculator = FuelCalculator()

    private val _telemetry = MutableStateFlow(CarTelemetry())
    val telemetry: StateFlow<CarTelemetry> = _telemetry.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private var isStreaming = false

    /**
     * Повна ініціалізація ELM327 та з'єднання з шиною ЕБУ автомобіля.
     */
    suspend fun initializeAdapter(deviceName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionStatus.value = ConnectionStatus.InitializingElm("Перезавантаження адаптера (ATZ)…")
            val atz = bluetoothManager.sendCommand(ObdConstants.CMD_RESET, timeoutMs = 2500)
            delay(800)

            _connectionStatus.value = ConnectionStatus.InitializingElm("Налаштування протоколу (ATE0, ATSP0)…")
            bluetoothManager.sendCommand(ObdConstants.CMD_ECHO_OFF)
            bluetoothManager.sendCommand(ObdConstants.CMD_LINEFEED_OFF)
            bluetoothManager.sendCommand(ObdConstants.CMD_SPACES_OFF)
            bluetoothManager.sendCommand(ObdConstants.CMD_HEADERS_OFF)
            bluetoothManager.sendCommand(ObdConstants.CMD_SET_PROTOCOL_AUTO)

            _connectionStatus.value = ConnectionStatus.InitializingElm("Зчитування напруги АКБ…")
            val voltageResp = bluetoothManager.sendCommand(ObdConstants.CMD_READ_VOLTAGE)
            val voltage = ObdParser.parseVoltage(voltageResp) ?: 12.6f

            _connectionStatus.value = ConnectionStatus.InitializingElm("Підключення до ЕБУ авто…")
            // Перший тестовий запит до ЕБУ для встановлення зв'язку за протоколом
            val initBusResp = bluetoothManager.sendCommand("0100", timeoutMs = 4000)
            if (initBusResp.contains("UNABLE TO CONNECT", ignoreCase = true) ||
                initBusResp.contains("BUS INIT: ... ERROR", ignoreCase = true)
            ) {
                _connectionStatus.value = ConnectionStatus.Error("Не вдалося підключитися до ЕБУ автомобіля. Перевірте запалювання!")
                return@withContext false
            }

            fuelCalculator.reset()
            _telemetry.value = CarTelemetry(batteryVoltage = voltage)
            _connectionStatus.value = ConnectionStatus.Connected(deviceName)
            true
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error("Помилка ініціалізації: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Безперервний цикл опитування параметрів автомобіля.
     */
    suspend fun startPollingLoop() = withContext(Dispatchers.IO) {
        if (isStreaming) return@withContext
        isStreaming = true

        var slowCycleCounter = 0
        var currentSpeed = 0
        var currentRpm = 0
        var currentCoolant = 90
        var currentLoad = 0f
        var currentThrottle = 0f
        var currentMaf = 0f
        var currentFuelLevel = 50f
        var currentVoltage = _telemetry.value.batteryVoltage

        try {
            while (isActive && isStreaming && bluetoothManager.isConnected) {
                // 1. Швидкість (PID 010D)
                val speedRaw = bluetoothManager.sendCommand(ObdConstants.PID_VEHICLE_SPEED, 800)
                ObdParser.parseSpeed(speedRaw)?.let { currentSpeed = it }

                // 2. Оберти двигуна (PID 010C)
                val rpmRaw = bluetoothManager.sendCommand(ObdConstants.PID_ENGINE_RPM, 800)
                ObdParser.parseRpm(rpmRaw)?.let { currentRpm = it }

                // 3. Витрата повітря MAF (PID 0110)
                val mafRaw = bluetoothManager.sendCommand(ObdConstants.PID_MAF_AIR_FLOW, 800)
                ObdParser.parseMaf(mafRaw)?.let { currentMaf = it }

                // 4. Навантаження на двигун (PID 0104)
                val loadRaw = bluetoothManager.sendCommand(ObdConstants.PID_ENGINE_LOAD, 800)
                ObdParser.parseEngineLoad(loadRaw)?.let { currentLoad = it }

                // 5. Положення дросельної заслінки (PID 0111)
                val throttleRaw = bluetoothManager.sendCommand(ObdConstants.PID_THROTTLE_POS, 800)
                ObdParser.parseThrottlePosition(throttleRaw)?.let { currentThrottle = it }

                // Повільні параметри (опитуємо раз на 15 циклів для економії часу шини)
                slowCycleCounter++
                if (slowCycleCounter >= 15) {
                    slowCycleCounter = 0

                    val coolantRaw = bluetoothManager.sendCommand(ObdConstants.PID_COOLANT_TEMP, 800)
                    ObdParser.parseCoolantTemp(coolantRaw)?.let { currentCoolant = it }

                    val fuelLevelRaw = bluetoothManager.sendCommand(ObdConstants.PID_FUEL_LEVEL, 800)
                    ObdParser.parseFuelLevel(fuelLevelRaw)?.let { currentFuelLevel = it }

                    val voltRaw = bluetoothManager.sendCommand(ObdConstants.CMD_READ_VOLTAGE, 800)
                    ObdParser.parseVoltage(voltRaw)?.let { currentVoltage = it }
                }

                // Розрахунок метрик витрати палива
                val fuelMetrics = fuelCalculator.updateTripMetrics(currentSpeed, currentMaf)

                // Оновлення стану телеметрії
                _telemetry.value = CarTelemetry(
                    speedKmh = currentSpeed,
                    engineRpm = currentRpm,
                    coolantTempC = currentCoolant,
                    engineLoadPercent = currentLoad,
                    throttlePositionPercent = currentThrottle,
                    batteryVoltage = currentVoltage,
                    mafAirFlowGramsPerSec = currentMaf,
                    fuelLevelPercent = currentFuelLevel,
                    instantaneousFuelFlowLitersPerHour = fuelMetrics.instantLitersPerHour,
                    instantaneousFuelConsumptionL100km = fuelMetrics.instantLitersPer100Km,
                    averageFuelConsumptionL100km = fuelMetrics.averageLitersPer100Km,
                    tripFuelUsedLiters = fuelMetrics.tripFuelUsedLiters,
                    tripDistanceKm = fuelMetrics.tripDistanceKm,
                    timestamp = System.currentTimeMillis()
                )

                delay(40) // Невелика пауза між пакетами опитування
            }
        } catch (_: CancellationException) {
            // Нормальна зупинка корутини
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error("Зв'язок перервано: ${e.localizedMessage}")
        } finally {
            isStreaming = false
        }
    }

    fun stopPolling() {
        isStreaming = false
    }

    /**
     * Зчитування збережених кодів помилок (DTC) двигуна.
     */
    suspend fun readDiagnosticCodes(): List<DtcCode> = withContext(Dispatchers.IO) {
        val wasStreaming = isStreaming
        if (wasStreaming) stopPolling()
        delay(150)

        try {
            val response = bluetoothManager.sendCommand(ObdConstants.CMD_READ_DTC, timeoutMs = 3000)
            ObdParser.parseDtcCodes(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Скидання кодів помилок та вимкнення індикатора Check Engine.
     */
    suspend fun clearDiagnosticCodes(): Boolean = withContext(Dispatchers.IO) {
        val wasStreaming = isStreaming
        if (wasStreaming) stopPolling()
        delay(150)

        try {
            val response = bluetoothManager.sendCommand(ObdConstants.CMD_CLEAR_DTC, timeoutMs = 3000)
            val clean = ObdParser.sanitize(response)
            clean.contains("44") || clean.contains("OK")
        } catch (e: Exception) {
            false
        }
    }
}
