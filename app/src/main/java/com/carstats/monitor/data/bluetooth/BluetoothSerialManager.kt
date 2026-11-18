package com.carstats.monitor.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Менеджер Bluetooth Classic з'єднання для обміну даними через RFCOMM (Serial Port Profile).
 */
class BluetoothSerialManager(private val context: Context) {

    companion object {
        // Стандартний UUID для Serial Port Profile (SPP) у Bluetooth Classic
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    val isBluetoothAvailable: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    val isConnected: Boolean
        get() = socket?.isConnected == true

    /**
     * Повертає список попередньо спарених (bonded) Bluetooth-пристроїв.
     */
    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<BluetoothDeviceModel> {
        if (!isBluetoothEnabled) return emptyList()
        return bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceModel(
                name = device.name ?: "Невідомий пристрій",
                address = device.address,
                isBonded = true
            )
        } ?: emptyList()
    }

    /**
     * Підключається до пристрою за MAC-адресою у фоновому потоці.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(macAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        disconnect()

        val adapter = bluetoothAdapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth не підтримується"))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth вимкнено"))
        }

        try {
            // Зупиняємо сканування перед з'єднанням, щоб вивільнити пропускну здатність
            adapter.cancelDiscovery()

            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            val btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            btSocket.connect()

            socket = btSocket
            inputStream = btSocket.inputStream
            outputStream = btSocket.outputStream

            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    /**
     * Відправляє рядок команди до ELM327 і чекає на відповідь до появи символу '>' або таймауту.
     */
    suspend fun sendCommand(command: String, timeoutMs: Long = 2000): String = withContext(Dispatchers.IO) {
        val out = outputStream ?: throw IllegalStateException("Немає з'єднання: потік виводу недоступний")
        val input = inputStream ?: throw IllegalStateException("Немає з'єднання: потік вводу недоступний")

        // Очищаємо залишки старого буфера, якщо є
        while (input.available() > 0) {
            input.read()
        }

        // Відправляємо команду, закінчуючи символом повернення каретки \r
        val commandPayload = (command.trim() + "\r").toByteArray(Charsets.US_ASCII)
        out.write(commandPayload)
        out.flush()

        val responseBuilder = StringBuilder()
        val buffer = ByteArray(256)

        val result = withTimeoutOrNull(timeoutMs) {
            while (true) {
                if (input.available() > 0) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead > 0) {
                        val chunk = String(buffer, 0, bytesRead, Charsets.US_ASCII)
                        responseBuilder.append(chunk)

                        // ELM327 сигналізує про завершення відповіді символом '>'
                        if (responseBuilder.contains(">")) {
                            break
                        }
                    }
                } else {
                    kotlinx.coroutines.delay(15)
                }
            }
            responseBuilder.toString()
        }

        val rawResponse = result ?: responseBuilder.toString()

        // Очищаємо отриману відповідь від символу prompt '>' та зайвих переносів
        rawResponse.replace(">", "").trim()
    }

    /**
     * Закриває з'єднання та звільняє ресурси сокета.
     */
    fun disconnect() {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}

        inputStream = null
        outputStream = null
        socket = null
    }
}
