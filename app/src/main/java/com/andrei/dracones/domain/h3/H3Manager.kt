package com.andrei.dracones.domain.h3

import com.google.android.gms.maps.model.LatLng
import com.uber.h3core.H3Core

object H3Manager {
    private val h3 by lazy { H3Core.newSystemInstance() }
    private const val RESOLUTION = 10

    fun latLngToCell(latLng: LatLng): String {
        return h3.latLngToCellAddress(latLng.latitude, latLng.longitude, RESOLUTION)
    }

    fun cellToBoundary(h3Index: String): List<LatLng> {
        val boundary = h3.cellToBoundary(h3Index)
        return boundary.map { LatLng(it.lat, it.lng) }
    }
}
