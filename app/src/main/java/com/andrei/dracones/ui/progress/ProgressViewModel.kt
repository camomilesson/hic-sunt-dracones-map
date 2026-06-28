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
                    _uiState.update { ProgressUiState(0, 0, 0) }
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
                // Block Parent (Res 10) holds up to 7 Res 11 cells
                val blockProgress = ((visitedInBlock.toFloat() / 7f) * 100).toInt().coerceAtMost(100)
                // Neighborhood Parent (Res 9) holds up to 49 Res 11 cells
                val neighborhoodProgress = ((visitedInNeighborhood.toFloat() / 49f) * 100).toInt().coerceAtMost(100)
                // District Parent (Res 8) holds up to 343 Res 11 cells
                val districtProgress = ((visitedInDistrict.toFloat() / 343f) * 100).toInt().coerceAtMost(100)

                _uiState.update {
                    ProgressUiState(
                        nearbyRegionProgress = blockProgress,
                        districtRegionProgress = neighborhoodProgress,
                        greaterRegionProgress = districtProgress
                    )
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }
}
