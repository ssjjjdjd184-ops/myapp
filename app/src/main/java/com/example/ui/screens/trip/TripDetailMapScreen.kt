package com.example.ui.screens.trip

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InteractiveRouteMap
import com.example.ui.components.SubMetricItem
import com.example.ui.viewmodel.TripsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailMapScreen(
    tripId: Long,
    viewModel: TripsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTrip by viewModel.selectedTrip.collectAsStateWithLifecycle()
    val routePoints by viewModel.selectedTripRoutePoints.collectAsStateWithLifecycle()
    val tripExpenses by viewModel.selectedTripExpenses.collectAsStateWithLifecycle()
    val pref = viewModel.prefManager
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault())

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseTitle by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("Fuel") }
    var expenseAmountText by remember { mutableStateOf("") }
    var expenseNotes by remember { mutableStateOf("") }

    val trip = selectedTrip

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "Trip Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val startLat = trip?.startLatitude
                        val startLon = trip?.startLongitude
                        val endLat = trip?.endLatitude
                        val endLon = trip?.endLongitude

                        val uri = if (startLat != null && startLon != null && endLat != null && endLon != null) {
                            Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$startLat,$startLon&destination=$endLat,$endLon")
                        } else if (startLat != null && startLon != null) {
                            Uri.parse("geo:$startLat,$startLon?q=$startLat,$startLon(${trip?.name ?: "Trip"})")
                        } else {
                            Uri.parse("geo:0,0?q=maps")
                        }
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(mapIntent)
                    }) {
                        Icon(Icons.Default.Map, contentDescription = "Open in Google Maps")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (trip == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val odoDistance = (trip.endOdometer - trip.startOdometer).coerceAtLeast(0.0)
        val distKm = trip.totalGpsDistanceMeters / 1000.0
        val costPerKm = if (distKm > 0.05) trip.totalTripExpense / distKm else 0.0

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // INTERACTIVE ROUTE MAP
            InteractiveRouteMap(
                points = routePoints,
                currentLocation = null,
                modifier = Modifier.height(300.dp),
                onOpenGoogleMaps = {
                    val startLat = trip.startLatitude
                    val startLon = trip.startLongitude
                    val endLat = trip.endLatitude
                    val endLon = trip.endLongitude

                    val uri = if (startLat != null && startLon != null && endLat != null && endLon != null) {
                        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$startLat,$startLon&destination=$endLat,$endLon")
                    } else if (startLat != null && startLon != null) {
                        Uri.parse("geo:$startLat,$startLon?q=$startLat,$startLon(${trip.name})")
                    } else {
                        Uri.parse("geo:0,0?q=maps")
                    }
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(mapIntent)
                }
            )

            // TRIP METRICS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRIP METRICS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(Date(trip.startTimeMillis)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total GPS Distance", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = pref.formatDistance(trip.totalGpsDistanceMeters),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (odoDistance > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Odometer Distance", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = String.format("%.2f %s", odoDistance, pref.distanceUnit.value),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubMetricItem(label = "Bike Distance", value = pref.formatDistance(trip.bikeDistanceMeters))
                        SubMetricItem(label = "Walking", value = pref.formatDistance(trip.walkingDistanceMeters))
                        SubMetricItem(label = "Duration", value = "${trip.durationSeconds / 60}m ${trip.durationSeconds % 60}s")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubMetricItem(label = "Avg Speed", value = pref.formatSpeed(trip.avgSpeedKmh))
                        SubMetricItem(label = "Max Speed", value = pref.formatSpeed(trip.maxSpeedKmh))
                        SubMetricItem(label = "Cost/km", value = pref.formatMoney(costPerKm))
                    }

                    if (trip.startOdometer > 0 || trip.endOdometer > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Start Odometer: ${trip.startOdometer} ${pref.distanceUnit.value}", fontSize = 12.sp)
                            Text("End Odometer: ${trip.endOdometer} ${pref.distanceUnit.value}", fontSize = 12.sp)
                        }
                    }
                }
            }

            // TRIP EXPENSES BREAKDOWN CARD
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
                        Column {
                            Text(
                                text = "TRIP EXPENSES",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total: ${pref.formatMoney(trip.totalTripExpense)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { showAddExpenseDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Cost", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Categorized costs summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubMetricItem(label = "Fuel", value = pref.formatMoney(trip.fuelCost))
                        SubMetricItem(label = "Toll", value = pref.formatMoney(trip.tollCost))
                        SubMetricItem(label = "Parking", value = pref.formatMoney(trip.parkingCost))
                        SubMetricItem(label = "Other", value = pref.formatMoney(trip.otherCost))
                    }

                    if (tripExpenses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Expense Items", style = MaterialTheme.typography.labelSmall)

                        tripExpenses.forEach { exp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(exp.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(exp.category, fontSize = 11.sp, color = Color.Gray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(pref.formatMoney(exp.amount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(
                                        onClick = { viewModel.deleteTripExpense(exp) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (trip.notes.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Trip Notes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(trip.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    // ADD TRIP EXPENSE DIALOG
    if (showAddExpenseDialog) {
        val categories = listOf("Fuel", "Toll", "Parking", "Food", "Other")
        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Add Trip Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Expense Title") },
                        placeholder = { Text("e.g. Highway Toll / Petrol Pump") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expenseAmountText,
                        onValueChange = { expenseAmountText = it },
                        label = { Text("Amount (${pref.currency.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = expenseCategory == cat,
                                onClick = { expenseCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = expenseNotes,
                        onValueChange = { expenseNotes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentTrip = trip
                        val amount = expenseAmountText.toDoubleOrNull() ?: 0.0
                        if (currentTrip != null && amount > 0) {
                            viewModel.addTripExpense(
                                tripId = currentTrip.id,
                                title = expenseTitle.ifBlank { expenseCategory },
                                category = expenseCategory,
                                amount = amount,
                                notes = expenseNotes
                            )
                        }
                        showAddExpenseDialog = false
                    }
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
