package com.carstats.monitor.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carstats.monitor.data.db.TripEntity
import com.carstats.monitor.ui.theme.CockpitBackground
import com.carstats.monitor.ui.theme.CockpitBorder
import com.carstats.monitor.ui.theme.CockpitSurface
import com.carstats.monitor.ui.theme.ElectricCyan
import com.carstats.monitor.ui.theme.SportRed
import com.carstats.monitor.ui.theme.TextMuted
import com.carstats.monitor.ui.theme.TextPrimary
import com.carstats.monitor.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripsScreen(
    trips: List<TripEntity>,
    onDeleteTrip: (TripEntity) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "ЖУРНАЛ ПОЇЗДОК",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Історія маршрутів, витрати палива та статистики",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Історія поїздок порожня",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Розпочніть поїздку на вкладці 'Дашборд'",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips) { trip ->
                    TripCardItem(
                        trip = trip,
                        onDelete = { onDeleteTrip(trip) },
                        onShare = { shareTripSummary(context, trip) }
                    )
                }
            }
        }
    }
}

@Composable
fun TripCardItem(
    trip: TripEntity,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(trip.startTime))

    val durationMinutes = trip.durationSeconds / 60
    val durationText = "${durationMinutes} хв"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CockpitSurface)
            .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = trip.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateString,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Row {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поділитися",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Видалити",
                            tint = SportRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricSnippet(title = "Дистанція", value = String.format(Locale.US, "%.1f км", trip.distanceKm))
                MetricSnippet(title = "Тривалість", value = durationText)
                MetricSnippet(title = "Паливо", value = String.format(Locale.US, "%.2f л", trip.fuelUsedLiters))
                MetricSnippet(title = "Сер. витрата", value = String.format(Locale.US, "%.1f л/100", trip.avgConsumptionL100km))
            }
        }
    }
}

@Composable
fun MetricSnippet(title: String, value: String) {
    Column {
        Text(text = title, color = TextMuted, fontSize = 10.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun shareTripSummary(context: Context, trip: TripEntity) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val text = """
        🚗 Звіт про поїздку: ${trip.title}
        Дата: ${dateFormat.format(Date(trip.startTime))}
        Дистанція: ${String.format(Locale.US, "%.1f", trip.distanceKm)} км
        Витрачено палива: ${String.format(Locale.US, "%.2f", trip.fuelUsedLiters)} л
        Середня витрата: ${String.format(Locale.US, "%.1f", trip.avgConsumptionL100km)} л/100км
        Тривалість: ${trip.durationSeconds / 60} хв
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Звіт про поїздку CarStats")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Поділитися звітом"))
}
