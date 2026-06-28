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
import com.andrei.dracones.data.repository.MapThemeRepository
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
        when (key) {
            "fog_opacity" -> {
                val opacity = sharedPrefs.getFloat("fog_opacity", 0.80f)
                _uiState.update { it.copy(fogOpacity = opacity) }
            }
            "show_businesses" -> {
                val show = sharedPrefs.getBoolean("show_businesses", true)
                _uiState.update { it.copy(showBusinesses = show) }
            }
            "show_transit" -> {
                val show = sharedPrefs.getBoolean("show_transit", true)
                _uiState.update { it.copy(showTransit = show) }
            }
            "show_other_poi" -> {
                val show = sharedPrefs.getBoolean("show_other_poi", true)
                _uiState.update { it.copy(showOtherPoi = show) }
            }
            "map_theme" -> {
                val theme = sharedPrefs.getString("map_theme", "Default") ?: "Default"
                _uiState.update { it.copy(mapTheme = theme) }
            }
            "fog_color_name" -> {
                val colorName = sharedPrefs.getString("fog_color_name", "Default") ?: "Default"
                _uiState.update { it.copy(fogColorName = colorName) }
            }
            "traveler_name" -> {
                val name = sharedPrefs.getString("traveler_name", "") ?: ""
                _uiState.update { it.copy(travelerName = name) }
            }
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
        val showBusinesses = sharedPrefs.getBoolean("show_businesses", true)
        val showTransit = sharedPrefs.getBoolean("show_transit", true)
        val showOtherPoi = sharedPrefs.getBoolean("show_other_poi", true)
        val mapTheme = sharedPrefs.getString("map_theme", "Default") ?: "Default"
        val fogColorName = sharedPrefs.getString("fog_color_name", "Default") ?: "Default"
        val travelerName = sharedPrefs.getString("traveler_name", "") ?: ""

        _uiState.update { it.copy(
            fogOpacity = initialOpacity,
            showBusinesses = showBusinesses,
            showTransit = showTransit,
            showOtherPoi = showOtherPoi,
            mapTheme = mapTheme,
            fogColorName = fogColorName,
            travelerName = travelerName,
        ) }

        val database = AppDatabase.getDatabase(application)
        repository = ExplorationRepository(database.visitedCellDao())
        val themeRepository = MapThemeRepository(database.mapThemeCacheDao())

        viewModelScope.launch {
            _uiState.update { it.copy(isThemesLoading = true, themesErrorMessage = null) }
            val cached = themeRepository.getCachedThemes()
            if (cached != null) {
                _uiState.update { it.copy(availableThemes = cached) }
                Log.d(TAG, "Loaded cached themes: ${cached.map { it.name }}")
            } else {
                _uiState.update { it.copy(availableThemes = themeRepository.fallbackThemes) }
                Log.d(TAG, "No cached themes, using fallback themes.")
            }
            try {
                com.andrei.dracones.domain.diagnostics.CrashReporter.log("Theme synchronization started")
                val networkThemes = themeRepository.fetchThemesFromNetwork()
                _uiState.update { it.copy(availableThemes = networkThemes, isThemesLoading = false) }
                com.andrei.dracones.domain.diagnostics.CrashReporter.log("Theme synchronization succeeded")
            } catch (e: Exception) {
                com.andrei.dracones.domain.diagnostics.CrashReporter.log("Theme synchronization failed: ${e.message}")
                Log.e(TAG, "Network themes fetch failed, keeping existing themes", e)
                
                // Record non-fatal if it's an unexpected parsing/serialization or formatting failure
                val isNetworkIssue = e is java.io.IOException || e is retrofit2.HttpException
                if (!isNetworkIssue) {
                    com.andrei.dracones.domain.diagnostics.CrashReporter.recordException(e)
                }

                _uiState.update { it.copy(
                    isThemesLoading = false,
                    themesErrorMessage = "Unable to sync latest themes. Operating in offline mode."
                ) }
            }
        }
        
        repository.allVisitedCells
            .onEach { entities ->
                Log.d(TAG, "Loading ${entities.size} visited cells")
                val h3Indices = entities.map { it.h3Index }
                val mergedRegions = H3Manager.cellsToMergedRegions(h3Indices)

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
                    visitedRegionOutlines = mergedRegions.exploredOutlines,
                    unexploredPockets = mergedRegions.unexploredPockets
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

        com.andrei.dracones.domain.diagnostics.CrashReporter.log("Tracking started")

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
        com.andrei.dracones.domain.diagnostics.CrashReporter.log("Tracking stopped")
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
                isWaitingForLocation = false,
                focusedRegion = null
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

    fun setShowBusinesses(show: Boolean) {
        sharedPrefs.edit { putBoolean("show_businesses", show) }
        _uiState.update { it.copy(showBusinesses = show) }
    }

    fun setShowTransit(show: Boolean) {
        sharedPrefs.edit { putBoolean("show_transit", show) }
        _uiState.update { it.copy(showTransit = show) }
    }

    fun setShowOtherPoi(show: Boolean) {
        sharedPrefs.edit { putBoolean("show_other_poi", show) }
        _uiState.update { it.copy(showOtherPoi = show) }
    }

    fun setMapTheme(theme: String) {
        sharedPrefs.edit { putString("map_theme", theme) }
        _uiState.update { it.copy(mapTheme = theme) }
    }

    fun setFogColorName(colorName: String) {
        sharedPrefs.edit { putString("fog_color_name", colorName) }
        _uiState.update { it.copy(fogColorName = colorName) }
    }

    fun setTravelerName(name: String) {
        sharedPrefs.edit { putString("traveler_name", name) }
        _uiState.update { it.copy(travelerName = name) }
    }

    fun setFocusedRegion(parentH3Index: String?, resolution: Int?) {
        if (parentH3Index == null || resolution == null) {
            _uiState.update { it.copy(focusedRegion = null) }
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val boundary = H3Manager.cellToBoundary(parentH3Index)
            _uiState.update {
                it.copy(
                    focusedRegion = FocusedRegionUiModel(
                        parentH3Index = parentH3Index,
                        resolution = resolution,
                        boundary = boundary
                    )
                )
            }
        }
    }

    fun clearFocusedRegion() {
        if (_uiState.value.focusedRegion != null) {
            _uiState.update { it.copy(focusedRegion = null) }
            Log.d(TAG, "Focused region cleared")
        }
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        stopTracking()
    }
}
