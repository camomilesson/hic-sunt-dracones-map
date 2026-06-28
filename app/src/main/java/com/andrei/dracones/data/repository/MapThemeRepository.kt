package com.andrei.dracones.data.repository

import android.util.Log
import com.andrei.dracones.data.model.MapStyleRuleModel
import com.andrei.dracones.data.model.MapThemeModel
import com.andrei.dracones.data.persistence.MapThemeCacheDao
import com.andrei.dracones.data.persistence.MapThemeCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class MapThemeRepository(private val cacheDao: MapThemeCacheDao) {

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
            try {
                val cache = cacheDao.getCache()
                if (cache != null) {
                    json.decodeFromString<List<MapThemeModel>>(cache.jsonPayload)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("HSD", "Failed to load cached themes", e)
                null
            }
        }
    }

    suspend fun fetchThemesFromNetwork(): List<MapThemeModel> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://gist.githubusercontent.com/camomilesson/1221849a8add935f72f9c59d21e288dc/raw/ef7a742d845fd97cb8418636109f0d1b63094ba9/map-themes.json")
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val payload = connection.inputStream.bufferedReader().use { it.readText() }
                    // Validate JSON before saving
                    val parsed = json.decodeFromString<List<MapThemeModel>>(payload)
                    
                    // Save to cache
                    cacheDao.saveCache(
                        MapThemeCacheEntity(
                            jsonPayload = payload,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                    Log.d("HSD", "Successfully downloaded and cached ${parsed.size} map themes")
                    parsed
                } else {
                    throw Exception("HTTP error code: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("HSD", "Failed to fetch map themes from network", e)
                throw e
            } finally {
                connection?.disconnect()
            }
        }
    }
}
