package com.example.ui.screens.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.components.CategoryBreakdown
import com.example.ui.components.ProgressRing
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.SubMetricItem
import com.example.ui.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedTimeFilter.collectAsStateWithLifecycle()
    val pref = viewModel.prefManager

    val timeFilters = listOf("Today", "Yesterday", "This week", "This month", "Last month", "All time")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeFilters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { viewModel.setTimeFilter(filter) },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        // 1. TRIP REPORT CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TRIP REPORT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("${state.tripReport.tripsCount} Trips", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Distance", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = pref.formatDistance(state.tripReport.totalDistanceMeters),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Trip Expenses", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = pref.formatMoney(state.tripReport.tripExpenses),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SubMetricItem(label = "Bike Distance", value = pref.formatDistance(state.tripReport.bikeDistanceMeters))
                    SubMetricItem(label = "Walking", value = pref.formatDistance(state.tripReport.walkingDistanceMeters))
                    SubMetricItem(
                        label = "Duration",
                        value = "${state.tripReport.totalDurationSeconds / 3600}h ${(state.tripReport.totalDurationSeconds % 3600) / 60}m"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SubMetricItem(label = "Avg Speed", value = pref.formatSpeed(state.tripReport.avgSpeedKmh))
                    SubMetricItem(label = "Max Speed", value = pref.formatSpeed(state.tripReport.maxSpeedKmh))
                    SubMetricItem(label = "Avg Cost/km", value = pref.formatMoney(state.tripReport.avgCostPerKm))
                }
            }
        }

        // 2. EXPENSE REPORT CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXPENSE REPORT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Total Period Expenses", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = pref.formatMoney(state.expenseReport.totalExpenses),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )

                if (state.expenseReport.dailyTotals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Daily Spending", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    SimpleBarChart(
                        data = state.expenseReport.dailyTotals,
                        barColor = Color(0xFFEF4444),
                        valueFormatter = { pref.formatMoney(it) }
                    )
                }

                if (state.expenseReport.categoryTotals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Category Breakdown", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryBreakdown(
                        categories = state.expenseReport.categoryTotals,
                        valueFormatter = { pref.formatMoney(it) }
                    )
                }
            }
        }

        // 3. SAVINGS REPORT CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SAVINGS REPORT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Total Savings", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = pref.formatMoney(state.savingsReport.totalSavings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SubMetricItem(label = "Deposits", value = pref.formatMoney(state.savingsReport.deposits))
                        Spacer(modifier = Modifier.height(4.dp))
                        SubMetricItem(label = "Withdrawals", value = pref.formatMoney(state.savingsReport.withdrawals))
                        Spacer(modifier = Modifier.height(4.dp))
                        SubMetricItem(label = "Accrued Interest", value = "+${pref.formatMoney(state.savingsReport.interestEarned)}")
                    }

                    ProgressRing(
                        progressPercent = state.savingsReport.targetProgress,
                        modifier = Modifier.size(90.dp),
                        label = "Goals"
                    )
                }
            }
        }

        // 4. CREDIT REPORT CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Handshake, contentDescription = null, tint = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CREDIT & LENDING REPORT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Outstanding", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = pref.formatMoney(state.creditReport.outstanding),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.creditReport.overdue > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (state.creditReport.overdue > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Overdue Amount", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                            Text(
                                text = pref.formatMoney(state.creditReport.overdue),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SubMetricItem(label = "Total Lent", value = pref.formatMoney(state.creditReport.moneyLent))
                    SubMetricItem(label = "Payments Collected", value = pref.formatMoney(state.creditReport.paymentsReceived))
                    SubMetricItem(label = "Interest Accrued", value = pref.formatMoney(state.creditReport.interest))
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}
