package com.andrei.dracones.ui.map

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
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

    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("dracones_settings", Context.MODE_PRIVATE)

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "fog_opacity") {
            val opacity = sharedPrefs.getFloat("fog_opacity", 0.80f)
            _uiState.update { it.copy(fogOpacity = opacity) }
            Log.d(TAG, "Fog opacity updated from shared preferences change: $opacity")
        }
    }

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val boundaryCache = mutableMapOf<String, List<LatLng>>()
    private val recentH3Cells = ArrayDeque<String>(3)

    companion object {
        private const val TAG = "HSD"
        const val DEFAULT_EXPLORATION_ZOOM = 17f
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
        val initialOpacity = sharedPrefs.getFloat("fog_opacity", 0.80f)
        _uiState.update { it.copy(fogOpacity = initialOpacity) }

        val database = AppDatabase.getDatabase(application)
        repository = ExplorationRepository(database.visitedCellDao())
        
        repository.allVisitedCells
            .onEach { entities ->
                Log.d(TAG, "Loading ${entities.size} visited cells")
                val h3Indices = entities.map { it.h3Index }
                val mergedOutlines = H3Manager.cellsToMultiPolygonOutlines(h3Indices)

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
                _uiState.update { it.copy(
                    visitedCells = uiModels,
                    visitedRegionOutlines = mergedOutlines
                ) }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    fun setFollowing(enabled: Boolean) {
        _uiState.update { it.copy(isFollowingUser = enabled) }
    }

    fun startTracking() {
        if (_uiState.value.isTracking) return

        _uiState.update { it.copy(
            isTracking = true, 
            isFollowingUser = true, 
            isWaitingForLocation = true,
            shouldApplyDefaultZoom = true,
            permissionMessage = null 
        ) }
        
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
        _uiState.update { it.copy(isTracking = false, isFollowingUser = false, isWaitingForLocation = false) }
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

    fun onInitialDiveCompleted() {
        _uiState.update { it.copy(shouldApplyDefaultZoom = false) }
    }

    private suspend fun processNewLocation(latLng: LatLng) {
        val h3Index = withContext(Dispatchers.Default) {
            H3Manager.latLngToCell(latLng)
        }

        val alreadyInRecent = synchronized(recentH3Cells) {
            recentH3Cells.contains(h3Index)
        }

        if (!alreadyInRecent) {
            withContext(Dispatchers.IO) {
                repository.markCellVisited(h3Index)
            }
            synchronized(recentH3Cells) {
                if (recentH3Cells.size >= 3) {
                    recentH3Cells.removeFirst()
                }
                recentH3Cells.addLast(h3Index)
            }
            Log.d(TAG, "New cell processed via tracking: $h3Index (Buffer: $recentH3Cells)")
        }

        _uiState.update { 
            it.copy(
                lastKnownLocation = latLng,
                lastVisitedH3Index = h3Index,
                isWaitingForLocation = false
            )
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
            synchronized(recentH3Cells) {
                recentH3Cells.clear()
            }
            repository.clearAll()
        }
    }

    fun setFogOpacity(opacity: Float) {
        sharedPrefs.edit { putFloat("fog_opacity", opacity) }
        _uiState.update { it.copy(fogOpacity = opacity) }
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        stopTracking()
    }
}
