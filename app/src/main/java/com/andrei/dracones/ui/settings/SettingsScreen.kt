package com.andrei.dracones.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Slider
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrei.dracones.ui.map.MapViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    // Reusing MapViewModel for simplicity as it already has access to repository and handles caches
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Clear explored territory?") },
            text = { Text("This will permanently delete all visited cells from this device. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearVisitedCells()
                        showDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Hic Sunt Dracones",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Uncover the world around you.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Fog Settings Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Fog Opacity: ${(uiState.fogOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = uiState.fogOpacity,
                    onValueChange = { viewModel.setFogOpacity(it) },
                    valueRange = 0.5f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fog Color Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Fog Color",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Parchment", "Silvery", "Blue").forEach { colorName ->
                        val isSelected = uiState.fogColorName == colorName
                        if (isSelected) {
                            Button(
                                onClick = { viewModel.setFogColorName(colorName) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(colorName)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.setFogColorName(colorName) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(colorName)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map Style Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Map Style",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themeNames = uiState.availableThemes.map { it.name }.ifEmpty { listOf("Default", "Parchment") }
                    themeNames.forEach { themeName ->
                        val isSelected = uiState.mapTheme.equals(themeName, ignoreCase = true)
                        if (isSelected) {
                            Button(
                                onClick = { viewModel.setMapTheme(themeName) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(themeName)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.setMapTheme(themeName) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(themeName)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map Elements Visibility Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Map Elements Visibility",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Show Businesses", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = uiState.showBusinesses,
                        onCheckedChange = { viewModel.setShowBusinesses(it) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Show Transit", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = uiState.showTransit,
                        onCheckedChange = { viewModel.setShowTransit(it) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Show Attractions & POIs", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = uiState.showOtherPoi,
                        onCheckedChange = { viewModel.setShowOtherPoi(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("Clear Exploration Data")
            }
        }
    }
}
