package com.carstats.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carstats.monitor.domain.model.CarTelemetry
import com.carstats.monitor.ui.components.RealtimeChart
import com.carstats.monitor.ui.theme.CockpitBackground
import com.carstats.monitor.ui.theme.EcoGreen
import com.carstats.monitor.ui.theme.ElectricCyan
import com.carstats.monitor.ui.theme.SportAmber
import com.carstats.monitor.ui.theme.SportRed
import com.carstats.monitor.ui.theme.TextPrimary
import com.carstats.monitor.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun GraphsScreen(
    telemetry: CarTelemetry,
    speedHistory: List<Float>,
    rpmHistory: List<Float>,
    coolantHistory: List<Float>,
    fuelRateHistory: List<Float>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "ГРАФІКИ ТЕЛЕМЕТРІЇ",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Динаміка ключових показників у реальному часі",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Графік швидкості
        RealtimeChart(
            title = "Швидкість",
            points = speedHistory,
            currentValue = telemetry.speedKmh.toString(),
            unit = "км/год",
            lineColor = ElectricCyan
        )

        // Графік обертів RPM
        RealtimeChart(
            title = "Оберти двигуна",
            points = rpmHistory,
            currentValue = telemetry.engineRpm.toString(),
            unit = "об/хв",
            lineColor = SportAmber
        )

        // Графік витрати палива
        RealtimeChart(
            title = "Миттєва витрата палива",
            points = fuelRateHistory,
            currentValue = String.format(Locale.US, "%.1f", telemetry.instantaneousFuelFlowLitersPerHour),
            unit = "л/год",
            lineColor = EcoGreen
        )

        // Графік температури охолоджуючої рідини
        RealtimeChart(
            title = "Температура антифризу",
            points = coolantHistory,
            currentValue = telemetry.coolantTempC.toString(),
            unit = "°C",
            lineColor = if (telemetry.coolantTempC > 100) SportRed else ElectricCyan
        )
    }
}
