package com.andrei.dracones.ui.map

import com.google.android.gms.maps.model.LatLng

data class VisitedCellUiModel(
    val h3Index: String,
    val boundary: List<LatLng>
)

data class MapUiState(
    val explorerName: String = "",
    val initialCameraPosition: CameraPositionState = CameraPositionState(),
    val visitedCells: List<VisitedCellUiModel> = emptyList(),
    val isTracking: Boolean = false,
    val isFollowingUser: Boolean = false,
    val isWaitingForLocation: Boolean = false,
    val shouldApplyDefaultZoom: Boolean = false,
    val permissionMessage: String? = null,
    val lastKnownLocation: LatLng? = null,
    val lastVisitedH3Index: String? = null
)

data class CameraPositionState(
    val latitude: Double = 0.0, // Center of the world (Atlantic)
    val longitude: Double = 0.0,
    val zoom: Float = 2f
)
