package com.carstats.monitor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : NavigationItem("dashboard", "Дашборд", Icons.Default.Speed)
    data object Graphs : NavigationItem("graphs", "Графіки", Icons.Default.ShowChart)
    data object Trips : NavigationItem("trips", "Поїздки", Icons.Default.DirectionsCar)
    data object Diagnostics : NavigationItem("diagnostics", "Діагностика", Icons.Default.Build)
}
