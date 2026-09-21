package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.TripRoutePoint
import android.content.Intent
import android.net.Uri

@Composable
fun InteractiveRouteMap(
    points: List<TripRoutePoint>,
    currentLocation: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
    onOpenGoogleMaps: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val startPoint = points.firstOrNull()
    val endPoint = points.lastOrNull()

    // Compute bounding box
    val minLat = remember(points) { points.minOfOrNull { it.latitude } ?: 0.0 }
    val maxLat = remember(points) { points.maxOfOrNull { it.latitude } ?: 0.0 }
    val minLon = remember(points) { points.minOfOrNull { it.longitude } ?: 0.0 }
    val maxLon = remember(points) { points.maxOfOrNull { it.longitude } ?: 0.0 }

    val latSpan = remember(minLat, maxLat) { (maxLat - minLat).coerceAtLeast(0.0002) }
    val lonSpan = remember(minLon, maxLon) { (maxLon - minLon).coerceAtLeast(0.0002) }

    fun fitRoute() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 10f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            if (points.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No GPS route points recorded yet",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "GPS points will appear here once tracking starts",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val padding = 40f

                    // Draw subtle coordinate grid
                    val gridLines = 4
                    for (i in 1..gridLines) {
                        val gx = w * (i.toFloat() / (gridLines + 1))
                        val gy = h * (i.toFloat() / (gridLines + 1))
                        drawLine(Color(0x1AFFFFFF), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                        drawLine(Color(0x1AFFFFFF), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                    }

                    fun toScreenX(lon: Double): Float {
                        val norm = ((lon - minLon) / lonSpan).toFloat()
                        val baseX = padding + norm * (w - 2 * padding)
                        return (baseX - w / 2) * scale + w / 2 + offsetX
                    }

                    fun toScreenY(lat: Double): Float {
                        val norm = ((lat - minLat) / latSpan).toFloat()
                        // Invert latitude so north is up
                        val baseY = (h - padding) - norm * (h - 2 * padding)
                        return (baseY - h / 2) * scale + h / 2 + offsetY
                    }

                    // Draw route lines connecting points
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val x1 = toScreenX(p1.longitude)
                        val y1 = toScreenY(p1.latitude)
                        val x2 = toScreenX(p2.longitude)
                        val y2 = toScreenY(p2.latitude)

                        val segmentColor = when (p2.movementType) {
                            "Bike" -> Color(0xFF00E676) // Emerald Green for Bike
                            "Walking" -> Color(0xFFFF9800) // Amber for Walking
                            else -> Color(0xFF64B5F6) // Soft Cyan/Blue for Stopped/Cruise
                        }

                        drawLine(
                            color = segmentColor,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 6f * scale.coerceIn(0.8f, 2.5f)
                        )
                    }

                    // Draw Start Pin
                    startPoint?.let { sp ->
                        val sx = toScreenX(sp.longitude)
                        val sy = toScreenY(sp.latitude)
                        drawCircle(Color(0xFF22C55E), radius = 14f, center = Offset(sx, sy))
                        drawCircle(Color.White, radius = 6f, center = Offset(sx, sy))
                    }

                    // Draw End Pin
                    endPoint?.let { ep ->
                        val ex = toScreenX(ep.longitude)
                        val ey = toScreenY(ep.latitude)
                        drawCircle(Color(0xFFEF4444), radius = 14f, center = Offset(ex, ey))
                        drawCircle(Color.White, radius = 6f, center = Offset(ex, ey))
                    }

                    // Draw Live Current Location if active
                    currentLocation?.let { (curLat, curLon) ->
                        val cx = toScreenX(curLon)
                        val cy = toScreenY(curLat)
                        drawCircle(Color(0x553B82F6), radius = 24f, center = Offset(cx, cy))
                        drawCircle(Color(0xFF3B82F6), radius = 10f, center = Offset(cx, cy))
                        drawCircle(Color.White, radius = 4f, center = Offset(cx, cy))
                    }
                }
            }

            // Legend at top left
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF00E676), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bike", color = Color.White, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF9800), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Walking", color = Color.White, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start", color = Color.White, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("End", color = Color.White, fontSize = 11.sp)
            }

            // Map Controls at top right
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = { scale = (scale * 1.3f).coerceAtMost(10f) },
                    modifier = Modifier.size(36.dp).background(Color(0xDD1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                IconButton(
                    onClick = { scale = (scale / 1.3f).coerceAtLeast(0.5f) },
                    modifier = Modifier.size(36.dp).background(Color(0xDD1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                IconButton(
                    onClick = { fitRoute() },
                    modifier = Modifier.size(36.dp).background(Color(0xDD1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "Fit route", tint = Color.White)
                }
            }

            // Bottom action: Open in Google Maps fallback
            Button(
                onClick = onOpenGoogleMaps,
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomCenter),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("OPEN IN GOOGLE MAPS", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
