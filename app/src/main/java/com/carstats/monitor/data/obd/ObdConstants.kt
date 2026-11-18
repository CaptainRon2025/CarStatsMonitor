package com.carstats.monitor.data.obd

/**
 * Константи команд ELM327 та стандартні PID протоколу OBD-II.
 */
object ObdConstants {
    // Команди ініціалізації ELM327
    const val CMD_RESET = "ATZ"
    const val CMD_ECHO_OFF = "ATE0"
    const val CMD_LINEFEED_OFF = "ATL0"
    const val CMD_SPACES_OFF = "ATS0"
    const val CMD_HEADERS_OFF = "ATH0"
    const val CMD_SET_PROTOCOL_AUTO = "ATSP0"
    const val CMD_READ_VOLTAGE = "ATRV"

    // Стандартні PID режиму 01 (Поточні дані)
    const val PID_ENGINE_LOAD = "0104"      // Розрахункове навантаження (%)
    const val PID_COOLANT_TEMP = "0105"     // Температура охолоджуючої рідини (°C)
    const val PID_ENGINE_RPM = "010C"       // Оберти двигуна (RPM)
    const val PID_VEHICLE_SPEED = "010D"    // Швидкість (км/год)
    const val PID_INTAKE_AIR_TEMP = "010F"  // Температура повітря на впуску (°C)
    const val PID_MAF_AIR_FLOW = "0110"     // Витрата повітря через MAF (г/с)
    const val PID_THROTTLE_POS = "0111"     // Положення дросельної заслінки (%)
    const val PID_FUEL_LEVEL = "012F"       // Рівень палива в баку (%)

    // Режими діагностики помилок
    const val CMD_READ_DTC = "03"           // Читання збережених кодів несправностей
    const val CMD_CLEAR_DTC = "04"          // Скидання кодів несправностей і лампи Check Engine
}
