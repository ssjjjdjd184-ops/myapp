package com.example.ui.screens.trip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InteractiveRouteMap
import com.example.ui.components.SubMetricItem
import com.example.ui.viewmodel.TripsViewModel
import android.content.Intent
import android.net.Uri

@Composable
fun LiveTripScreen(
    viewModel: TripsViewModel,
    onTripFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val pref = viewModel.prefManager
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    var showStartDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }

    // Start Dialog states
    var startTripName by remember { mutableStateOf("") }
    var startOdometerText by remember { mutableStateOf("") }
    var startNotes by remember { mutableStateOf("") }

    // Stop Dialog states
    var endOdometerText by remember { mutableStateOf("") }
    var fuelCostText by remember { mutableStateOf("") }
    var tollCostText by remember { mutableStateOf("") }
    var parkingCostText by remember { mutableStateOf("") }
    var otherCostText by remember { mutableStateOf("") }
    var stopNotes by remember { mutableStateOf("") }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!hasLocationPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Location Permission Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Please grant location permission to track your GPS route and speed.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Status & Accuracy Chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (liveState.tripState == "RUNNING") Color(0x1A10B981) else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            when (liveState.tripState) {
                                "RUNNING" -> Color(0xFF10B981)
                                "PAUSED" -> Color(0xFFF59E0B)
                                else -> Color.Gray
                            },
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (liveState.tripState) {
                        "RUNNING" -> "RECORDING TRIP"
                        "PAUSED" -> "TRIP PAUSED"
                        else -> "READY TO START"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = when (liveState.tripState) {
                        "RUNNING" -> Color(0xFF10B981)
                        "PAUSED" -> Color(0xFFF59E0B)
                        else -> Color.Gray
                    }
                )
            }
            Text(
                text = liveState.gpsStatusMessage,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // BIG SPEED DISPLAY
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Movement Mode Badge
                Surface(
                    color = when (liveState.movementType) {
                        "Bike" -> Color(0xFF00E676)
                        "Walking" -> Color(0xFFFF9800)
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (liveState.movementType) {
                                "Bike" -> Icons.Default.DirectionsBike
                                "Walking" -> Icons.Default.DirectionsWalk
                                else -> Icons.Default.PauseCircle
                            },
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = liveState.movementType.uppercase(),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Huge speed number
                Text(
                    text = String.format("%.1f", liveState.currentSpeedKmh),
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pref.speedUnit.value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Secondary metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SubMetricItem(label = "Duration", value = formatDuration(liveState.durationSeconds))
                    SubMetricItem(label = "Avg Speed", value = pref.formatSpeed(liveState.avgSpeedKmh))
                    SubMetricItem(label = "Max Speed", value = pref.formatSpeed(liveState.maxSpeedKmh))
                }
            }
        }

        // DISTANCE BREAKDOWN CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DISTANCE BREAKDOWN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total GPS Distance", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = pref.formatDistance(liveState.totalDistanceMeters),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SubMetricItem(
                        label = "Bike Distance",
                        value = pref.formatDistance(liveState.bikeDistanceMeters)
                    )
                    SubMetricItem(
                        label = "Walking Distance",
                        value = pref.formatDistance(liveState.walkingDistanceMeters)
                    )
                }
            }
        }

        // LIVE ROUTE MINI-MAP
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LIVE ROUTE PATH",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                InteractiveRouteMap(
                    points = liveState.routePoints,
                    currentLocation = if (liveState.currentLatitude != null && liveState.currentLongitude != null) {
                        Pair(liveState.currentLatitude!!, liveState.currentLongitude!!)
                    } else null,
                    modifier = Modifier.height(240.dp),
                    onOpenGoogleMaps = {
                        val lat = liveState.currentLatitude ?: 0.0
                        val lon = liveState.currentLongitude ?: 0.0
                        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Current+Location)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(mapIntent)
                    }
                )
            }
        }

        // ACTION BUTTONS (START / PAUSE / RESUME / STOP)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (liveState.tripState) {
                "NOT_STARTED", "COMPLETED" -> {
                    Button(
                        onClick = { showStartDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START NEW TRIP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                "RUNNING" -> {
                    Button(
                        onClick = { viewModel.pauseTrip() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PAUSE", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showStopDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FINISH", fontWeight = FontWeight.Bold)
                    }
                }
                "PAUSED" -> {
                    Button(
                        onClick = { viewModel.resumeTrip() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESUME", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showStopDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FINISH", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // START TRIP DIALOG
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Start Trip", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = startTripName,
                        onValueChange = { startTripName = it },
                        label = { Text("Trip Name (Optional)") },
                        placeholder = { Text("e.g. Morning Ride to Work") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = startOdometerText,
                        onValueChange = { startOdometerText = it },
                        label = { Text("Start Odometer Reading (${pref.distanceUnit.value})") },
                        placeholder = { Text("e.g. 12500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = startNotes,
                        onValueChange = { startNotes = it },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("Weather, route plans, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val odo = startOdometerText.toDoubleOrNull() ?: 0.0
                        viewModel.startTrip(startTripName, odo, startNotes)
                        showStartDialog = false
                    }
                ) {
                    Text("Start GPS Tracking")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // STOP TRIP DIALOG
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Complete Trip", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Enter final trip metrics and expenses:")
                    OutlinedTextField(
                        value = endOdometerText,
                        onValueChange = { endOdometerText = it },
                        label = { Text("End Odometer Reading (${pref.distanceUnit.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fuelCostText,
                        onValueChange = { fuelCostText = it },
                        label = { Text("Fuel Cost (${pref.currency.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tollCostText,
                        onValueChange = { tollCostText = it },
                        label = { Text("Toll Cost (${pref.currency.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = parkingCostText,
                        onValueChange = { parkingCostText = it },
                        label = { Text("Parking Cost (${pref.currency.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = otherCostText,
                        onValueChange = { otherCostText = it },
                        label = { Text("Other Cost (${pref.currency.value})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = stopNotes,
                        onValueChange = { stopNotes = it },
                        label = { Text("Trip Summary Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val endOdo = endOdometerText.toDoubleOrNull() ?: 0.0
                        val fuel = fuelCostText.toDoubleOrNull() ?: 0.0
                        val toll = tollCostText.toDoubleOrNull() ?: 0.0
                        val parking = parkingCostText.toDoubleOrNull() ?: 0.0
                        val other = otherCostText.toDoubleOrNull() ?: 0.0

                        viewModel.stopTrip(
                            endOdometer = endOdo,
                            fuelCost = fuel,
                            tollCost = toll,
                            parkingCost = parking,
                            otherCost = other,
                            notes = stopNotes
                        ) { savedTrip ->
                            showStopDialog = false
                            onTripFinished(savedTrip.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save & Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Keep Running")
                }
            }
        )
    }
}
