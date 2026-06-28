package com.andrei.dracones.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrei.dracones.ui.map.MapViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    // Reusing MapViewModel for simplicity as it already has access to repository and handles caches
    viewModel: MapViewModel = viewModel(),
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
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
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Hic Sunt Dracones",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Uncover the world around you.",
                style = MaterialTheme.typography.bodyMedium,
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Fog Settings Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Fog Opacity: ${(uiState.fogOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = uiState.fogOpacity,
                    onValueChange = { viewModel.setFogOpacity(it) },
                    valueRange = 0.5f..1.0f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map Style Section (Moved above Fog Color Section)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Map Style",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Sorting and displaying themes dynamically (Default, Parchment, Night)
                    val baseThemes = uiState.availableThemes
                    val themeOrder = listOf("default", "parchment", "night")
                    val sortedThemes = baseThemes.sortedBy { theme ->
                        val index = themeOrder.indexOf(theme.id.lowercase())
                        if (index != -1) index else Int.MAX_VALUE
                    }
                    val displayThemes = sortedThemes.ifEmpty {
                        listOf(
                            com.andrei.dracones.data.model.MapThemeModel("default", "Default", "Standard", emptyList()),
                            com.andrei.dracones.data.model.MapThemeModel("parchment", "Parchment", "Warm", emptyList())
                        )
                    }

                    displayThemes.forEach { theme ->
                        val themeName = theme.name
                        val isSelected = uiState.mapTheme.equals(themeName, ignoreCase = true)
                        
                        // Map Style buttons with custom colors matching the dominant colors
                        val buttonLabel = if (themeName.equals("Default", ignoreCase = true)) "Blue" else themeName
                        val dominantColor = when (themeName.lowercase()) {
                            "parchment" -> Color(0xFFC0C9B2) // Muted sage/olive
                            "night" -> Color(0xFF242F3E)      // Deep dark navy
                            else -> Color(0xFF2E3A52)         // Subdued blue (matching default blue fog)
                        }

                        ThemeOptionButton(
                            displayName = buttonLabel,
                            selected = isSelected,
                            color = dominantColor,
                            onClick = { viewModel.setMapTheme(themeName) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fog Color Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Fog Color",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Display fog colors in order: Default, Parchment, Silver
                    val fogColors = listOf(
                        "Default" to Color(0xFF2E3A52),      // Subdued blue
                        "Parchment" to Color(0xFFE0D2B8),    // Warm parchment beige
                        "Silver" to Color(0xFFC5D1D6)        // Silvery mist
                    )

                    fogColors.forEach { (colorName, colorVal) ->
                        val isSelected = uiState.fogColorName == colorName
                        ThemeOptionButton(
                            displayName = colorName,
                            selected = isSelected,
                            color = colorVal,
                            onClick = { viewModel.setFogColorName(colorName) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map Elements Visibility Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Map Elements Visibility",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Show Businesses", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = uiState.showBusinesses,
                        onCheckedChange = { viewModel.setShowBusinesses(it) },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Show Transit", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = uiState.showTransit,
                        onCheckedChange = { viewModel.setShowTransit(it) },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Show Attractions & POIs", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = uiState.showOtherPoi,
                        onCheckedChange = { viewModel.setShowOtherPoi(it) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            ) {
                Text("Clear Exploration Data")
            }
        }
    }
}

@Composable
fun ThemeOptionButton(
    displayName: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        // High contrast text over filled background
        if (color == Color(0xFFC0C9B2) || color == Color(0xFFE0D2B8) || color == Color(0xFFC5D1D6)) {
            Color(0xFF2C3524) // Dark text for lighter pastel colors
        } else {
            Color.White
        }
    } else {
        color
    }

    val backgroundColor = if (selected) color else Color.Transparent
    val borderStroke = if (selected) null else BorderStroke(2.dp, color)

    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor,
        contentColor = contentColor,
        border = borderStroke,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
