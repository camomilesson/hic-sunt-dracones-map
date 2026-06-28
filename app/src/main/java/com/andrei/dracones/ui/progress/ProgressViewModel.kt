package com.andrei.dracones.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andrei.dracones.data.persistence.AppDatabase
import com.andrei.dracones.data.repository.ExplorationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExplorationRepository
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExplorationRepository(database.visitedCellDao())

        repository.allVisitedCells
            .onEach { entities ->
                if (entities.isEmpty()) {
                    _uiState.update { ProgressUiState() }
                    return@onEach
                }

                // Find the anchor cell (the most recently visited H3 cell)
                val anchor = entities.maxByOrNull { it.lastVisitedAt } ?: return@onEach
                val blockParent = anchor.blockParentH3
                val neighborhoodParent = anchor.neighborhoodParentH3
                val districtParent = anchor.districtParentH3

                // Count visited Resolution-11 cells belonging to each active parent
                val visitedInBlock = entities.count { it.blockParentH3 == blockParent }
                val visitedInNeighborhood = entities.count { it.neighborhoodParentH3 == neighborhoodParent }
                val visitedInDistrict = entities.count { it.districtParentH3 == districtParent }

                // Calculate progress percentages
                val blockProgress = ((visitedInBlock.toFloat() / 7f) * 100).toInt().coerceAtMost(100)
                val neighborhoodProgress = ((visitedInNeighborhood.toFloat() / 49f) * 100).toInt().coerceAtMost(100)
                val districtProgress = ((visitedInDistrict.toFloat() / 343f) * 100).toInt().coerceAtMost(100)

                // Additional Advanced Stats
                // 1. Total cells and estimated area (Res 11 cell is ~0.00249 km²)
                val totalCells = entities.size
                val totalArea = totalCells * 0.00249

                // 2. Discoveries over time (Today, Week, Month rolling timeframes)
                val now = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L
                val sevenDaysMs = 7 * oneDayMs
                val thirtyDaysMs = 30 * oneDayMs

                val todayCount = entities.count { it.firstVisitedAt >= now - oneDayMs }
                val weekCount = entities.count { it.firstVisitedAt >= now - sevenDaysMs }
                val monthCount = entities.count { it.firstVisitedAt >= now - thirtyDaysMs }

                // 3. Total footsteps sum
                val totalFootstepsSum = entities.sumOf { it.visitCount.toLong() }

                // 4. Most active single day (based on firstVisitedAt)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val discoveriesByDay = entities.groupBy { sdf.format(Date(it.firstVisitedAt)) }
                val mostActiveEntry = discoveriesByDay.maxByOrNull { it.value.size }
                val activeDayDate = mostActiveEntry?.key ?: "N/A"
                val activeDayCount = mostActiveEntry?.value?.size ?: 0

                _uiState.update {
                    ProgressUiState(
                        nearbyRegionProgress = blockProgress,
                        districtRegionProgress = neighborhoodProgress,
                        greaterRegionProgress = districtProgress,
                        totalCellsExplored = totalCells,
                        totalAreaKm2 = totalArea,
                        cellsToday = todayCount,
                        cellsThisWeek = weekCount,
                        cellsThisMonth = monthCount,
                        totalFootsteps = totalFootstepsSum,
                        mostActiveDayDate = activeDayDate,
                        mostActiveDayCount = activeDayCount
                    )
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }
}
