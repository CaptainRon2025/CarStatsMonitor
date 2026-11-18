package com.carstats.monitor.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carstats.monitor.ui.MainViewModel
import com.carstats.monitor.ui.screens.DashboardScreen
import com.carstats.monitor.ui.screens.DeviceDialog
import com.carstats.monitor.ui.screens.DiagnosticsScreen
import com.carstats.monitor.ui.screens.GraphsScreen
import com.carstats.monitor.ui.screens.TripsScreen
import com.carstats.monitor.ui.theme.CockpitBackground
import com.carstats.monitor.ui.theme.CockpitSurface
import com.carstats.monitor.ui.theme.ElectricCyan
import com.carstats.monitor.ui.theme.TextMuted
import com.carstats.monitor.ui.theme.TextSecondary

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavigationItem.Dashboard.route

    val telemetry by viewModel.telemetry.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val speedHistory by viewModel.speedHistory.collectAsState()
    val rpmHistory by viewModel.rpmHistory.collectAsState()
    val coolantHistory by viewModel.coolantHistory.collectAsState()
    val fuelRateHistory by viewModel.fuelRateHistory.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val isTripActive by viewModel.isTripActive.collectAsState()
    val dtcCodes by viewModel.dtcCodes.collectAsState()
    val isDtcScanning by viewModel.isDtcScanning.collectAsState()
    val dtcStatusMessage by viewModel.dtcStatusMessage.collectAsState()
    val bondedDevices by viewModel.bondedDevices.collectAsState()

    var showDeviceDialog by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavigationItem.Dashboard,
        NavigationItem.Graphs,
        NavigationItem.Trips,
        NavigationItem.Diagnostics
    )

    if (showDeviceDialog) {
        DeviceDialog(
            devices = bondedDevices,
            onDeviceSelected = { device ->
                showDeviceDialog = false
                viewModel.connectToDevice(device)
            },
            onRefresh = { viewModel.loadBondedDevices() },
            onDismiss = { showDeviceDialog = false }
        )
    }

    Scaffold(
        containerColor = CockpitBackground,
        bottomBar = {
            NavigationBar(
                containerColor = CockpitSurface
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(imageVector = item.icon, contentDescription = item.title)
                        },
                        label = {
                            Text(text = item.title, fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricCyan,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan.copy(alpha = 0.15f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavigationItem.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavigationItem.Dashboard.route) {
                DashboardScreen(
                    telemetry = telemetry,
                    connectionStatus = connectionStatus,
                    isTripActive = isTripActive,
                    onStartTrip = { viewModel.startTrip() },
                    onFinishTrip = { viewModel.finishTrip() },
                    onOpenDeviceDialog = {
                        viewModel.loadBondedDevices()
                        showDeviceDialog = true
                    }
                )
            }

            composable(NavigationItem.Graphs.route) {
                GraphsScreen(
                    telemetry = telemetry,
                    speedHistory = speedHistory,
                    rpmHistory = rpmHistory,
                    coolantHistory = coolantHistory,
                    fuelRateHistory = fuelRateHistory
                )
            }

            composable(NavigationItem.Trips.route) {
                TripsScreen(
                    trips = trips,
                    onDeleteTrip = { trip -> viewModel.deleteTrip(trip) }
                )
            }

            composable(NavigationItem.Diagnostics.route) {
                DiagnosticsScreen(
                    dtcCodes = dtcCodes,
                    isLoading = isDtcScanning,
                    statusMessage = dtcStatusMessage,
                    onReadCodes = { viewModel.readTroubleCodes() },
                    onClearCodes = { viewModel.clearTroubleCodes() }
                )
            }
        }
    }
}
