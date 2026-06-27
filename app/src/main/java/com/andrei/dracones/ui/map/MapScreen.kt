package com.andrei.dracones.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(
    onNavigateToProgress: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        viewModel.onLocationPermissionResult(granted)
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(uiState.initialCameraPosition.latitude, uiState.initialCameraPosition.longitude),
            uiState.initialCameraPosition.zoom
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Google Map layer
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) },
                onMapClick = { latLng -> viewModel.markCellVisited(latLng) }
            ) {
                uiState.visitedCells.forEach { cell ->
                    Polygon(
                        points = cell.boundary,
                        fillColor = Color.Blue.copy(alpha = 0.3f),
                        strokeColor = Color.Blue,
                        strokeWidth = 2f
                    )
                }

                uiState.lastKnownLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Current Position",
                        alpha = 0.8f
                    )
                }
            }

            // UI Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hic Sunt Dracones",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Uncover the world around you.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        uiState.permissionMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (uiState.isTracking) {
                            Text(
                                text = "Tracking active" + (uiState.lastVisitedH3Index?.let { " - $it" } ?: ""),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Text(
                                text = "Tracking stopped",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.explorerName,
                            onValueChange = { viewModel.onExplorerNameChanged(it) },
                            label = { Text("Explorer Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (uiState.isTracking) {
                                        viewModel.stopTracking()
                                    } else {
                                        val fineLocation = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        val coarseLocation = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (fineLocation && coarseLocation) {
                                            viewModel.startTracking()
                                        } else {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = if (uiState.isTracking) {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Text(if (uiState.isTracking) "Stop Tracking" else "Start Tracking")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onNavigateToProgress(uiState.explorerName) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Progress")
                            }
                            OutlinedButton(
                                onClick = { viewModel.clearVisitedCells() },
                                modifier = Modifier.padding(start = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }
    }
}
