package com.andrei.dracones.domain.h3

import com.google.android.gms.maps.model.LatLng
import com.uber.h3core.H3Core

object H3Manager {
    private val h3 by lazy { H3Core.newSystemInstance() }
    
    const val EXPLORATION_RESOLUTION = 11
    const val BLOCK_PARENT_RESOLUTION = 10
    const val NEIGHBORHOOD_PARENT_RESOLUTION = 9
    const val DISTRICT_PARENT_RESOLUTION = 8

    fun latLngToCell(latLng: LatLng): String {
        return h3.latLngToCellAddress(latLng.latitude, latLng.longitude, EXPLORATION_RESOLUTION)
    }

    fun cellToBoundary(h3Index: String): List<LatLng> {
        val boundary = h3.cellToBoundary(h3Index)
        return boundary.map { LatLng(it.lat, it.lng) }
    }

    fun getParent(h3Index: String, parentResolution: Int): String {
        return h3.cellToParentAddress(h3Index, parentResolution)
    }

    fun cellsToMultiPolygonOutlines(h3Indices: Collection<String>): List<List<LatLng>> {
        if (h3Indices.isEmpty()) return emptyList()
        val h3MultiPolygon = h3.cellAddressesToMultiPolygon(h3Indices, false)
        return h3MultiPolygon.map { loops ->
            val outerLoop = loops.firstOrNull() ?: emptyList()
            outerLoop.map { LatLng(it.lat, it.lng) }
        }
    }
}
