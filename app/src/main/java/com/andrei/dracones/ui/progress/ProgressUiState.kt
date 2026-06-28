package com.andrei.dracones.ui.progress

data class ProgressUiState(
    val nearbyRegionProgress: Int = 0,
    val districtRegionProgress: Int = 0,
    val greaterRegionProgress: Int = 0,
    val totalCellsExplored: Int = 0,
    val totalAreaKm2: Double = 0.0,
    val cellsToday: Int = 0,
    val cellsThisWeek: Int = 0,
    val cellsThisMonth: Int = 0,
    val totalFootsteps: Long = 0L,
    val mostActiveDayDate: String = "N/A",
    val mostActiveDayCount: Int = 0,
    val blockParentH3: String? = null,
    val neighborhoodParentH3: String? = null,
    val districtParentH3: String? = null
)
