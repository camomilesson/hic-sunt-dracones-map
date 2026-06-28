package com.andrei.dracones.ui.progress

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(),
    onNavigateToMap: ((parentH3Index: String, parentResolution: Int) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Discovery Statistics",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // SECTION 1: Exploration Progress
            Text(
                text = "Exploration Progress",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            StatProgressRow(
                label = "Nearby Block", 
                progress = uiState.nearbyRegionProgress,
                onClick = uiState.blockParentH3?.let { h3 ->
                    { onNavigateToMap?.invoke(h3, 10) }
                }
            )
            StatProgressRow(
                label = "Local Neighborhood", 
                progress = uiState.districtRegionProgress,
                onClick = uiState.neighborhoodParentH3?.let { h3 ->
                    { onNavigateToMap?.invoke(h3, 9) }
                }
            )
            StatProgressRow(
                label = "Greater District", 
                progress = uiState.greaterRegionProgress,
                onClick = uiState.districtParentH3?.let { h3 ->
                    { onNavigateToMap?.invoke(h3, 8) }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // SECTION 2: Total Coverage
            Text(
                text = "Total Coverage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            StatValueRow(label = "Total Cells Uncovered", value = "${uiState.totalCellsExplored}")
            StatValueRow(
                label = "Total Area Uncovered", 
                value = String.format(Locale.getDefault(), "~%.4f km²", uiState.totalAreaKm2)
            )
            StatValueRow(label = "Movement Logs Collected", value = "${uiState.totalFootsteps}")

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // SECTION 3: Recent Activity
            Text(
                text = "Recent Discoveries",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            StatValueRow(label = "Uncovered Today", value = "${uiState.cellsToday} cells")
            StatValueRow(label = "Uncovered This Week", value = "${uiState.cellsThisWeek} cells")
            StatValueRow(label = "Uncovered This Month", value = "${uiState.cellsThisMonth} cells")

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // SECTION 4: Personal Bests
            Text(
                text = "Personal Records",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            StatValueRow(label = "Most Active Day", value = uiState.mostActiveDayDate)
            StatValueRow(label = "Peak Daily Discoveries", value = "${uiState.mostActiveDayCount} cells")
        }
    }
}

@Composable
fun StatProgressRow(
    label: String, 
    progress: Int,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = "$progress%", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StatValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
