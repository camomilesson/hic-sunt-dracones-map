package com.andrei.dracones.ui.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andrei.dracones.data.persistence.AppDatabase
import com.andrei.dracones.data.repository.ExplorationRepository
import com.andrei.dracones.domain.h3.H3Manager
import com.andrei.dracones.data.location.LocationTracker
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExplorationRepository
    private val locationTracker = LocationTracker(application)
    private var trackingJob: Job? = null

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val boundaryCache = mutableMapOf<String, List<LatLng>>()

    companion object {
        private const val TAG = "HSD"
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExplorationRepository(database.visitedCellDao())
        
        repository.allVisitedCells
            .onEach { entities ->
                Log.d(TAG, "Loading ${entities.size} visited cells")
                val uiModels = synchronized(boundaryCache) {
                    entities.map { entity ->
                        val boundary = boundaryCache.getOrPut(entity.h3Index) {
                            H3Manager.cellToBoundary(entity.h3Index)
                        }
                        VisitedCellUiModel(
                            h3Index = entity.h3Index,
                            boundary = boundary
                        )
                    }
                }
                _uiState.update { it.copy(visitedCells = uiModels) }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    fun onExplorerNameChanged(newName: String) {
        _uiState.update { it.copy(explorerName = newName) }
    }

    fun startTracking() {
        if (_uiState.value.isTracking) return

        _uiState.update { it.copy(isTracking = true, permissionMessage = null) }
        
        trackingJob = locationTracker.getLocationUpdates(5000L)
            .onEach { latLng ->
                processNewLocation(latLng)
            }
            .launchIn(viewModelScope)
        
        Log.d(TAG, "Location tracking started")
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.update { it.copy(isTracking = false) }
        Log.d(TAG, "Location tracking stopped")
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(permissionMessage = null) }
            startTracking()
        } else {
            _uiState.update { it.copy(permissionMessage = "Location permission is required for tracking.") }
        }
    }

    private suspend fun processNewLocation(latLng: LatLng) {
        val h3Index = withContext(Dispatchers.Default) {
            H3Manager.latLngToCell(latLng)
        }

        if (h3Index != _uiState.value.lastVisitedH3Index) {
            withContext(Dispatchers.IO) {
                repository.markCellVisited(h3Index)
            }
            _uiState.update { 
                it.copy(
                    lastKnownLocation = latLng,
                    lastVisitedH3Index = h3Index
                )
            }
            Log.d(TAG, "New cell discovered via tracking: $h3Index")
        } else {
            _uiState.update { it.copy(lastKnownLocation = latLng) }
        }
    }

    fun markCellVisited(latLng: LatLng) {
        viewModelScope.launch {
            val h3Index = withContext(Dispatchers.Default) {
                H3Manager.latLngToCell(latLng)
            }
            withContext(Dispatchers.IO) {
                repository.markCellVisited(h3Index)
            }
        }
    }

    fun clearVisitedCells() {
        viewModelScope.launch(Dispatchers.IO) {
            synchronized(boundaryCache) {
                boundaryCache.clear()
            }
            repository.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
