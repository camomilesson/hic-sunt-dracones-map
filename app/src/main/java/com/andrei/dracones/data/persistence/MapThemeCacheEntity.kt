package com.andrei.dracones.data.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_themes_cache")
data class MapThemeCacheEntity(
    @PrimaryKey
    val id: String = "latest_themes",
    val jsonPayload: String, // serialized List<MapThemeModel>
    val lastUpdated: Long
)
