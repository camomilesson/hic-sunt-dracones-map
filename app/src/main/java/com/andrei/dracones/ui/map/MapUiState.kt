package com.andrei.dracones.ui.map

import com.google.android.gms.maps.model.LatLng

data class VisitedCellUiModel(
    val h3Index: String,
    val boundary: List<LatLng>
)

data class MapUiState(
    val explorerName: String = "",
    val initialCameraPosition: CameraPositionState = CameraPositionState(),
    val visitedCells: List<VisitedCellUiModel> = emptyList()
)

data class CameraPositionState(
    val latitude: Double = 41.3851, // Barcelona
    val longitude: Double = 2.1734,
    val zoom: Float = 12f
)
