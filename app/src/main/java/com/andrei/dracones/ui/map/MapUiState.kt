package com.andrei.dracones.ui.map

data class MapUiState(
    val explorerName: String = "",
    val initialCameraPosition: CameraPositionState = CameraPositionState()
)

data class CameraPositionState(
    val latitude: Double = 41.3851, // Barcelona
    val longitude: Double = 2.1734,
    val zoom: Float = 12f
)
