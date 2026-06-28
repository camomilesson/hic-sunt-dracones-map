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
        return try {
            h3.latLngToCellAddress(latLng.latitude, latLng.longitude, EXPLORATION_RESOLUTION)
        } catch (e: Throwable) {
            com.andrei.dracones.domain.diagnostics.CrashReporter.recordException(e)
            throw e
        }
    }

    fun cellToBoundary(h3Index: String): List<LatLng> {
        return try {
            val boundary = h3.cellToBoundary(h3Index)
            boundary.map { LatLng(it.lat, it.lng) }
        } catch (e: Throwable) {
            com.andrei.dracones.domain.diagnostics.CrashReporter.recordException(e)
            throw e
        }
    }

    fun getParent(h3Index: String, parentResolution: Int): String {
        return try {
            h3.cellToParentAddress(h3Index, parentResolution)
        } catch (e: Throwable) {
            com.andrei.dracones.domain.diagnostics.CrashReporter.recordException(e)
            throw e
        }
    }

    fun cellsToMergedRegions(h3Indices: Collection<String>): MergedExplorationRegions {
        if (h3Indices.isEmpty()) return MergedExplorationRegions(emptyList(), emptyList())
        return try {
            val h3MultiPolygon = h3.cellAddressesToMultiPolygon(h3Indices, false)
            val exploredOutlines = mutableListOf<List<LatLng>>()
            val unexploredPockets = mutableListOf<List<LatLng>>()

            for (loops in h3MultiPolygon) {
                val outerLoop = loops.firstOrNull() ?: continue
                exploredOutlines.add(outerLoop.map { LatLng(it.lat, it.lng) })
                
                if (loops.size > 1) {
                    for (i in 1 until loops.size) {
                        val innerLoop = loops[i]
                        unexploredPockets.add(innerLoop.map { LatLng(it.lat, it.lng) })
                    }
                }
            }
            MergedExplorationRegions(exploredOutlines, unexploredPockets)
        } catch (e: Throwable) {
            com.andrei.dracones.domain.diagnostics.CrashReporter.recordException(e)
            throw e
        }
    }
}

data class MergedExplorationRegions(
    val exploredOutlines: List<List<LatLng>>,
    val unexploredPockets: List<List<LatLng>>
)
