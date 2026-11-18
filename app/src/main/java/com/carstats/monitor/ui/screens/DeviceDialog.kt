package com.carstats.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carstats.monitor.data.bluetooth.BluetoothDeviceModel
import com.carstats.monitor.ui.theme.CockpitBorder
import com.carstats.monitor.ui.theme.CockpitSurface
import com.carstats.monitor.ui.theme.CockpitSurfaceVariant
import com.carstats.monitor.ui.theme.ElectricCyan
import com.carstats.monitor.ui.theme.TextMuted
import com.carstats.monitor.ui.theme.TextPrimary
import com.carstats.monitor.ui.theme.TextSecondary

@Composable
fun DeviceDialog(
    devices: List<BluetoothDeviceModel>,
    onDeviceSelected: (BluetoothDeviceModel) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Виберіть ELM327",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Оновити список",
                        tint = ElectricCyan
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Пристрій повинен бути попередньо спарений у налаштуваннях Bluetooth телефону (код: 1234 або 0000).",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Спарених пристроїв не знайдено.\nПерейдіть у налаштування Bluetooth вашого телефону та підключіть 'OBDII'.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices) { device ->
                            val isElmCandidate = device.name.contains("OBD", ignoreCase = true) ||
                                    device.name.contains("ELM", ignoreCase = true)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isElmCandidate) CockpitSurfaceVariant else CockpitBorder.copy(alpha = 0.3f))
                                    .clickable { onDeviceSelected(device) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (isElmCandidate) ElectricCyan else TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = device.name,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = if (isElmCandidate) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = device.address,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати", color = TextSecondary)
            }
        }
    )
}
