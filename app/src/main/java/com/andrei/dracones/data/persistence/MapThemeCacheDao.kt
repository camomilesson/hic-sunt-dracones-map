package com.andrei.dracones.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MapThemeCacheDao {
    @Query("SELECT * FROM map_themes_cache WHERE id = 'latest_themes' LIMIT 1")
    suspend fun getCache(): MapThemeCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCache(cache: MapThemeCacheEntity)

    @Query("DELETE FROM map_themes_cache")
    suspend fun deleteCache()
}
