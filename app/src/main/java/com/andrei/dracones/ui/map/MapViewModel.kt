package com.andrei.dracones.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andrei.dracones.data.persistence.AppDatabase
import com.andrei.dracones.data.repository.ExplorationRepository
import com.andrei.dracones.domain.h3.H3Manager
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExplorationRepository
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val boundaryCache = mutableMapOf<String, List<LatLng>>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExplorationRepository(database.visitedCellDao())
        
        repository.allVisitedCells.onEach { entities ->
            val uiModels = entities.map { entity ->
                val boundary = boundaryCache.getOrPut(entity.h3Index) {
                    H3Manager.cellToBoundary(entity.h3Index)
                }
                VisitedCellUiModel(
                    h3Index = entity.h3Index,
                    boundary = boundary
                )
            }
            _uiState.update { it.copy(visitedCells = uiModels) }
        }.launchIn(viewModelScope)
    }

    fun onExplorerNameChanged(newName: String) {
        _uiState.update { it.copy(explorerName = newName) }
    }

    fun markCellVisited(latLng: LatLng) {
        val h3Index = H3Manager.latLngToCell(latLng)
        viewModelScope.launch {
            repository.markCellVisited(h3Index)
        }
    }

    fun clearVisitedCells() {
        viewModelScope.launch {
            boundaryCache.clear()
            repository.clearAll()
        }
    }
}
