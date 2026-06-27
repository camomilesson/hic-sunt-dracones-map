package com.andrei.dracones.ui.map

import androidx.lifecycle.ViewModel
import com.andrei.dracones.domain.h3.H3Manager
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MapViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onExplorerNameChanged(newName: String) {
        _uiState.update { it.copy(explorerName = newName) }
    }

    fun markCellVisited(latLng: LatLng) {
        val h3Index = H3Manager.latLngToCell(latLng)
        
        _uiState.update { state ->
            if (state.visitedCells.any { it.h3Index == h3Index }) {
                state
            } else {
                val boundary = H3Manager.cellToBoundary(h3Index)
                val newCell = VisitedCellUiModel(h3Index, boundary)
                state.copy(visitedCells = state.visitedCells + newCell)
            }
        }
    }

    fun clearVisitedCells() {
        _uiState.update { it.copy(visitedCells = emptyList()) }
    }
}
