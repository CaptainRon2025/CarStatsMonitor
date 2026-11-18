package com.carstats.monitor.data.obd

import com.carstats.monitor.domain.model.DtcCode
import com.carstats.monitor.domain.model.DtcSeverity

/**
 * Парсер текстових HEX-відповідей від адаптера ELM327 та ЕБУ автомобіля.
 */
object ObdParser {

    /**
     * Очищає відповідь від пробілів, переносів рядків та повертає чистий рядок у верхньому регістрі.
     */
    fun sanitize(response: String): String {
        return response.replace("\\s".toRegex(), "").uppercase()
    }

    /**
     * Перевіряє, чи є відповідь помилковою з боку ELM327 або ЕБУ.
     */
    fun isErrorResponse(response: String): Boolean {
        val clean = response.uppercase()
        return clean.contains("NODATA") ||
                clean.contains("UNABLETOCONNECT") ||
                clean.contains("BUSINIT") ||
                clean.contains("CANERROR") ||
                clean.contains("STOPPED") ||
                clean.contains("ERROR") ||
                clean.contains("?")
    }

    /**
     * Парсинг напруги акумулятора (відповідь на команду ATRV, наприклад "12.4V" або "14.1 V").
     */
    fun parseVoltage(raw: String): Float? {
        val regex = "([0-9]+\\.?[0-9]*)".toRegex()
        val match = regex.find(raw)
        return match?.value?.toFloatOrNull()
    }

    /**
     * Парсинг обертів двигуна (PID 010C, очікується "410CXXXX" -> ((A * 256) + B) / 4).
     */
    fun parseRpm(raw: String): Int? {
        val clean = sanitize(raw)
        val index = clean.indexOf("410C")
        if (index != -1 && clean.length >= index + 8) {
            val hexA = clean.substring(index + 4, index + 6)
            val hexB = clean.substring(index + 6, index + 8)
            val a = hexA.toIntOrNull(16) ?: return null
            val b = hexB.toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 4
        }
        return null
    }

    /**
     * Парсинг швидкості (PID 010D, очікується "410DXX" -> A км/год).
     */
    fun parseSpeed(raw: String): Int? {
        val clean = sanitize(raw)
        val index = clean.indexOf("410D")
        if (index != -1 && clean.length >= index + 6) {
            val hexA = clean.substring(index + 4, index + 6)
            return hexA.toIntOrNull(16)
        }
        return null
    }

    /**
     * Парсинг температури охолоджуючої рідини (PID 0105, очікується "4105XX" -> A - 40 °C).
     */
    fun parseCoolantTemp(raw: String): Int? {
        val clean = sanitize(raw)
        val index = clean.indexOf("4105")
        if (index != -1 && clean.length >= index + 6) {
            val hexA = clean.substring(index + 4, index + 6)
            val a = hexA.toIntOrNull(16) ?: return null
            return a - 40
        }
        return null
    }

    /**
     * Парсинг розрахункового навантаження на двигун (PID 0104, очікується "4104XX" -> (A * 100) / 255 %).
     */
    fun parseEngineLoad(raw: String): Float? {
        val clean = sanitize(raw)
        val index = clean.indexOf("4104")
        if (index != -1 && clean.length >= index + 6) {
            val hexA = clean.substring(index + 4, index + 6)
            val a = hexA.toIntOrNull(16) ?: return null
            return (a * 100f) / 255f
        }
        return null
    }

    /**
     * Парсинг положення дроселя (PID 0111, очікується "4111XX" -> (A * 100) / 255 %).
     */
    fun parseThrottlePosition(raw: String): Float? {
        val clean = sanitize(raw)
        val index = clean.indexOf("4111")
        if (index != -1 && clean.length >= index + 6) {
            val hexA = clean.substring(index + 4, index + 6)
            val a = hexA.toIntOrNull(16) ?: return null
            return (a * 100f) / 255f
        }
        return null
    }

    /**
     * Парсинг витрати повітря MAF (PID 0110, очікується "4110XXXX" -> ((A * 256) + B) / 100 г/с).
     */
    fun parseMaf(raw: String): Float? {
        val clean = sanitize(raw)
        val index = clean.indexOf("4110")
        if (index != -1 && clean.length >= index + 8) {
            val hexA = clean.substring(index + 4, index + 6)
            val hexB = clean.substring(index + 6, index + 8)
            val a = hexA.toIntOrNull(16) ?: return null
            val b = hexB.toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 100f
        }
        return null
    }

    /**
     * Парсинг рівня палива в баку (PID 012F, очікується "412FXX" -> (A * 100) / 255 %).
     */
    fun parseFuelLevel(raw: String): Float? {
        val clean = sanitize(raw)
        val index = clean.indexOf("412F")
        if (index != -1 && clean.length >= index + 6) {
            val hexA = clean.substring(index + 4, index + 6)
            val a = hexA.toIntOrNull(16) ?: return null
            return (a * 100f) / 255f
        }
        return null
    }

    /**
     * Парсинг кодів несправностей (DTC) з режиму 03.
     * Відповідь зазвичай починається з "43" і містить байти кодів (по 2 байти на помилку).
     */
    fun parseDtcCodes(raw: String): List<DtcCode> {
        val clean = sanitize(raw)
        if (isErrorResponse(clean) || !clean.contains("43")) {
            return emptyList()
        }

        val index = clean.indexOf("43")
        // Залишаємо байти після заголовка 43
        val payload = clean.substring(index + 2)
        val result = mutableListOf<DtcCode>()

        var i = 0
        while (i + 4 <= payload.length) {
            val codeHex = payload.substring(i, i + 4)
            i += 4

            if (codeHex == "0000") continue // Порожнє місце/заповнювач

            val byte1 = codeHex.substring(0, 2).toIntOrNull(16) ?: continue
            val byte2 = codeHex.substring(2, 4).toIntOrNull(16) ?: continue

            val prefix = when ((byte1 and 0xC0) shr 6) {
                0 -> "P" // Двигун / Трансмісія
                1 -> "C" // Шасі
                2 -> "B" // Кузов
                3 -> "U" // Мережа CAN/шина
                else -> "P"
            }

            val secondDigit = (byte1 and 0x30) shr 4
            val thirdDigit = byte1 and 0x0F
            val fourthDigit = (byte2 and 0xF0) shr 4
            val fifthDigit = byte2 and 0x0F

            val code = String.format("%s%d%X%X%X", prefix, secondDigit, thirdDigit, fourthDigit, fifthDigit)
            val description = getDtcDescription(code)
            result.add(
                DtcCode(
                    code = code,
                    description = description,
                    category = when (prefix) {
                        "P" -> "Двигун та КПП"
                        "C" -> "Шасі та підвіска"
                        "B" -> "Кузов та салон"
                        "U" -> "Електронні блоки / CAN"
                        else -> "Загальна помилка"
                    },
                    severity = if (code.startsWith("P03") || code.startsWith("P02")) DtcSeverity.CRITICAL else DtcSeverity.WARNING
                )
            )
        }

        return result
    }

    /**
     * Довідник найпоширеніших кодів помилок українською мовою.
     */
    private fun getDtcDescription(code: String): String {
        return when (code) {
            "P0100" -> "Несправність у колі датчика масової витрати повітря (MAF)"
            "P0101" -> "Діапазон/функціонування датчика витрати повітря (MAF)"
            "P0102" -> "Низький рівень сигналу датчика витрати повітря (MAF)"
            "P0115" -> "Несправність датчика температури охолоджуючої рідини"
            "P0118" -> "Високий рівень сигналу датчика температури антифризу"
            "P0120" -> "Несправність датчика положення дросельної заслінки"
            "P0130" -> "Несправність датчика кисню (Лямбда-зонд Банк 1, Датчик 1)"
            "P0133" -> "Повільний відгук датчика кисню (Банк 1, Датчик 1)"
            "P0171" -> "Занадто бідна паливо-повітряна суміш (Банк 1)"
            "P0172" -> "Занадто багата паливо-повітряна суміш (Банк 1)"
            "P0300" -> "Виявлено випадкові/множинні пропуски запалювання"
            "P0301" -> "Пропуски запалювання в циліндрі 1"
            "P0302" -> "Пропуски запалювання в циліндрі 2"
            "P0303" -> "Пропуски запалювання в циліндрі 3"
            "P0304" -> "Пропуски запалювання в циліндрі 4"
            "P0420" -> "Ефективність каталізатора нижче порогового значення (Банк 1)"
            "P0500" -> "Несправність датчика швидкості автомобіля (VSS)"
            "P0505" -> "Несправність системи регулювання холостого ходу (РХХ)"
            "P0562" -> "Низька напруга бортової мережі автомобіля"
            else -> "Код несправності системи OBD-II ($code). Зверніться до сервісного довідника."
        }
    }
}
