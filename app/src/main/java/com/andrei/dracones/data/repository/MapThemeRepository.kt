package com.andrei.dracones.data.repository

import android.util.Log
import com.andrei.dracones.data.model.MapStyleRuleModel
import com.andrei.dracones.data.model.MapThemeModel
import com.andrei.dracones.data.persistence.MapThemeCacheDao
import com.andrei.dracones.data.persistence.MapThemeCacheEntity
import com.andrei.dracones.data.remote.MapThemeApi
import com.andrei.dracones.data.remote.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MapThemeRepository(
    private val cacheDao: MapThemeCacheDao,
    private val mapThemeApi: MapThemeApi = RetrofitInstance.mapThemeApi
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val fallbackThemes: List<MapThemeModel> = listOf(
        MapThemeModel(
            id = "default",
            name = "Default",
            description = "Standard Google Maps styling.",
            style = emptyList()
        ),
        MapThemeModel(
            id = "parchment",
            name = "Parchment",
            description = "Warm old-map style for exploration.",
            style = listOf(
                MapStyleRuleModel(elementType = "geometry", stylers = listOf(mapOf("color" to "#f5f1e6"))),
                MapStyleRuleModel(elementType = "labels.text.fill", stylers = listOf(mapOf("color" to "#5c5344"))),
                MapStyleRuleModel(elementType = "labels.text.stroke", stylers = listOf(mapOf("color" to "#f5f1e6"))),
                MapStyleRuleModel(featureType = "administrative", elementType = "geometry.stroke", stylers = listOf(mapOf("color" to "#c9b2a6"))),
                MapStyleRuleModel(featureType = "landscape.natural", elementType = "geometry", stylers = listOf(mapOf("color" to "#c0c9b2"))),
                MapStyleRuleModel(featureType = "poi", elementType = "geometry", stylers = listOf(mapOf("color" to "#dfd2be"))),
                MapStyleRuleModel(featureType = "poi.park", elementType = "geometry", stylers = listOf(mapOf("color" to "#c0c9b2"))),
                MapStyleRuleModel(featureType = "road", elementType = "geometry", stylers = listOf(mapOf("color" to "#fdfcf8"))),
                MapStyleRuleModel(featureType = "road.highway", elementType = "geometry", stylers = listOf(mapOf("color" to "#f8c967"))),
                MapStyleRuleModel(featureType = "road.highway", elementType = "geometry.stroke", stylers = listOf(mapOf("color" to "#e9bc62"))),
                MapStyleRuleModel(featureType = "water", elementType = "geometry.fill", stylers = listOf(mapOf("color" to "#bacad6")))
            )
        )
    )

    suspend fun getCachedThemes(): List<MapThemeModel>? {
        return withContext(Dispatchers.IO) {
            val cache = try {
                cacheDao.getCache()
            } catch (e: Exception) {
                Log.e("HSD", "Failed to load cached themes from database", e)
                com.andrei.dracones.domain.diagnostics.CrashReporter.recordException(e)
                null
            }
            if (cache != null) {
                try {
                    json.decodeFromString<List<MapThemeModel>>(cache.jsonPayload)
                } catch (e: Exception) {
                    Log.e("HSD", "Failed to deserialize cached themes JSON", e)
                    com.andrei.dracones.domain.diagnostics.CrashReporter.recordException(e)
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun fetchThemesFromNetwork(): List<MapThemeModel> {
        return withContext(Dispatchers.IO) {
            try {
                val parsed = mapThemeApi.getMapThemes()
                val payload = json.encodeToString(parsed)
                
                // Save to cache
                cacheDao.saveCache(
                    MapThemeCacheEntity(
                        jsonPayload = payload,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
                Log.d("HSD", "Successfully downloaded and cached ${parsed.size} map themes via Retrofit")
                parsed
            } catch (e: Exception) {
                Log.e("HSD", "Failed to fetch map themes from network via Retrofit", e)
                throw e
            }
        }
    }
}
