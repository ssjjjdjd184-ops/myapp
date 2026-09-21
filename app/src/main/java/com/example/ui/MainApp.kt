package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.credit.CreditScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.expense.ExpensesScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.savings.SavingsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.trip.LiveTripScreen
import com.example.ui.screens.trip.TripDetailMapScreen
import com.example.ui.screens.trip.TripsScreen
import com.example.ui.theme.TripCalcTheme
import com.example.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()

    val dashboardViewModel: DashboardViewModel = viewModel()
    val tripsViewModel: TripsViewModel = viewModel()
    val expensesViewModel: ExpensesViewModel = viewModel()
    val savingsViewModel: SavingsViewModel = viewModel()
    val creditViewModel: CreditViewModel = viewModel()
    val reportsViewModel: ReportsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val liveTripState by tripsViewModel.liveState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Pair(Screen.Dashboard, Icons.Default.Dashboard),
        Pair(Screen.Trips, Icons.Default.DirectionsBike),
        Pair(Screen.Expenses, Icons.Default.ReceiptLong),
        Pair(Screen.Savings, Icons.Default.Savings),
        Pair(Screen.Credit, Icons.Default.Handshake),
        Pair(Screen.Reports, Icons.Default.BarChart)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.first.route }

    TripCalcTheme(themeMode = themeMode) {
        Scaffold(
            topBar = {
                if (currentRoute != Screen.TripMap.route + "/{tripId}") {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "TripCalc",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GPS & Money",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        actions = {
                            // Live Trip Status Pill if active
                            if (liveTripState.tripState == "RUNNING" || liveTripState.tripState == "PAUSED") {
                                Surface(
                                    modifier = Modifier
                                        .clickable { navController.navigate(Screen.LiveTrip.route) }
                                        .padding(end = 8.dp),
                                    color = if (liveTripState.tripState == "RUNNING") Color(0xFF10B981) else Color(0xFFF59E0B),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (liveTripState.tripState == "RUNNING") "● REC" else "❚❚ PAUSED",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tripsViewModel.prefManager.formatDistance(liveTripState.totalDistanceMeters),
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        tonalElevation = 8.dp
                    ) {
                        bottomNavItems.forEach { (screen, icon) ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = screen.title) },
                                label = { Text(screen.title, fontSize = 10.sp) },
                                selected = selected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToTrips = { navController.navigate(Screen.Trips.route) },
                        onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                        onNavigateToSavings = { navController.navigate(Screen.Savings.route) },
                        onNavigateToCredit = { navController.navigate(Screen.Credit.route) },
                        onNavigateToLiveTrip = { navController.navigate(Screen.LiveTrip.route) }
                    )
                }

                composable(Screen.Trips.route) {
                    TripsScreen(
                        viewModel = tripsViewModel,
                        onTripSelected = { tripId ->
                            navController.navigate("${Screen.TripMap.route}/$tripId")
                        },
                        onNavigateToLiveTrip = {
                            navController.navigate(Screen.LiveTrip.route)
                        }
                    )
                }

                composable(Screen.LiveTrip.route) {
                    LiveTripScreen(
                        viewModel = tripsViewModel,
                        onTripFinished = { tripId ->
                            navController.navigate("${Screen.TripMap.route}/$tripId") {
                                popUpTo(Screen.Trips.route)
                            }
                        }
                    )
                }

                composable(
                    route = "${Screen.TripMap.route}/{tripId}",
                    arguments = listOf(navArgument("tripId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                    TripDetailMapScreen(
                        tripId = tripId,
                        viewModel = tripsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Expenses.route) {
                    ExpensesScreen(viewModel = expensesViewModel)
                }

                composable(Screen.Savings.route) {
                    SavingsScreen(viewModel = savingsViewModel)
                }

                composable(Screen.Credit.route) {
                    CreditScreen(viewModel = creditViewModel)
                }

                composable(Screen.Reports.route) {
                    ReportsScreen(viewModel = reportsViewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}
