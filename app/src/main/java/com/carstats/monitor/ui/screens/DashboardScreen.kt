package com.carstats.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carstats.monitor.domain.model.CarTelemetry
import com.carstats.monitor.domain.model.ConnectionStatus
import com.carstats.monitor.ui.components.GaugeMeter
import com.carstats.monitor.ui.components.TelemetryCard
import com.carstats.monitor.ui.theme.CockpitBackground
import com.carstats.monitor.ui.theme.CockpitBorder
import com.carstats.monitor.ui.theme.CockpitSurface
import com.carstats.monitor.ui.theme.EcoGreen
import com.carstats.monitor.ui.theme.ElectricCyan
import com.carstats.monitor.ui.theme.SportAmber
import com.carstats.monitor.ui.theme.SportRed
import com.carstats.monitor.ui.theme.TextMuted
import com.carstats.monitor.ui.theme.TextPrimary
import com.carstats.monitor.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DashboardScreen(
    telemetry: CarTelemetry,
    connectionStatus: ConnectionStatus,
    isTripActive: Boolean,
    onStartTrip: () -> Unit,
    onFinishTrip: () -> Unit,
    onOpenDeviceDialog: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- 1. Верхній індикатор статусу підключення ---
        ConnectionStatusBar(
            connectionStatus = connectionStatus,
            voltage = telemetry.batteryVoltage,
            onConnectClick = onOpenDeviceDialog
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Головні прилади (Спідометр та Тахометр) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GaugeMeter(
                value = telemetry.speedKmh.toFloat(),
                minValue = 0f,
                maxValue = 220f,
                title = "Швидкість",
                unit = "км/год",
                activeColor = ElectricCyan,
                modifier = Modifier.weight(1f)
            )

            GaugeMeter(
                value = telemetry.engineRpm.toFloat(),
                minValue = 0f,
                maxValue = 7000f,
                warningThreshold = 5500f,
                title = "Оберти",
                unit = "RPM",
                activeColor = SportAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Картки ключових параметрів ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val fuelDisplay = if (telemetry.speedKmh > 5) {
                String.format(Locale.US, "%.1f", telemetry.instantaneousFuelConsumptionL100km)
            } else {
                String.format(Locale.US, "%.1f", telemetry.instantaneousFuelFlowLitersPerHour)
            }
            val fuelUnit = if (telemetry.speedKmh > 5) "л/100км" else "л/год"

            TelemetryCard(
                title = "Миттєва витрата",
                value = fuelDisplay,
                unit = fuelUnit,
                icon = Icons.Default.LocalGasStation,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )

            val coolantColor = when {
                telemetry.coolantTempC >= 105 -> SportRed
                telemetry.coolantTempC < 65 -> ElectricCyan
                else -> EcoGreen
            }

            TelemetryCard(
                title = "Охолодження",
                value = telemetry.coolantTempC.toString(),
                unit = "°C",
                icon = Icons.Default.Thermostat,
                accentColor = coolantColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryCard(
                title = "Навантаження",
                value = String.format(Locale.US, "%.0f", telemetry.engineLoadPercent),
                unit = "%",
                icon = Icons.Default.Speed,
                accentColor = SportAmber,
                modifier = Modifier.weight(1f)
            )

            TelemetryCard(
                title = "Напруга АКБ",
                value = String.format(Locale.US, "%.1f", telemetry.batteryVoltage),
                unit = "V",
                icon = Icons.Default.ElectricMeter,
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 4. Панель поточної поїздки (Бортовий комп'ютер) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ПОТОЧНА ПОЇЗДКА",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isTripActive) "🟢 Запис активний" else "⚪ Запис зупинено",
                            color = if (isTripActive) EcoGreen else TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { if (isTripActive) onFinishTrip() else onStartTrip() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTripActive) SportRed else ElectricCyan
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isTripActive) "Зупинити" else "Старт поїздки",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TripStatItem(
                        label = "Дистанція",
                        value = String.format(Locale.US, "%.1f", telemetry.tripDistanceKm),
                        unit = "км"
                    )
                    TripStatItem(
                        label = "Витрачено",
                        value = String.format(Locale.US, "%.2f", telemetry.tripFuelUsedLiters),
                        unit = "л"
                    )
                    TripStatItem(
                        label = "Сер. витрата",
                        value = String.format(Locale.US, "%.1f", telemetry.averageFuelConsumptionL100km),
                        unit = "л/100"
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBar(
    connectionStatus: ConnectionStatus,
    voltage: Float,
    onConnectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CockpitSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (statusColor, statusText) = when (connectionStatus) {
                    is ConnectionStatus.Connected -> Pair(EcoGreen, "Підключено: ${connectionStatus.deviceName}")
                    is ConnectionStatus.Connecting -> Pair(SportAmber, "З'єднання…")
                    is ConnectionStatus.InitializingElm -> Pair(SportAmber, connectionStatus.step)
                    is ConnectionStatus.Scanning -> Pair(ElectricCyan, "Пошук…")
                    is ConnectionStatus.Error -> Pair(SportRed, "Помилка")
                    is ConnectionStatus.Disconnected -> Pair(TextMuted, "Не підключено")
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(containerColor = CockpitBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (connectionStatus is ConnectionStatus.Connected) "Змінити" else "Підключити",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun TripStatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Text(
            text = "$value $unit",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
