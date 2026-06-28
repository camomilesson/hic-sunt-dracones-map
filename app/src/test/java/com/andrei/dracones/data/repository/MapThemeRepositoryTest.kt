package com.andrei.dracones.data.repository

import com.andrei.dracones.data.model.MapThemeModel
import com.andrei.dracones.data.persistence.MapThemeCacheDao
import com.andrei.dracones.data.persistence.MapThemeCacheEntity
import com.andrei.dracones.data.remote.MapThemeApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapThemeRepositoryTest {

    private class FakeMapThemeCacheDao : MapThemeCacheDao {
        var cachedEntity: MapThemeCacheEntity? = null

        override suspend fun getCache(): MapThemeCacheEntity? {
            return cachedEntity
        }

        override suspend fun saveCache(cache: MapThemeCacheEntity) {
            cachedEntity = cache
        }

        override suspend fun deleteCache() {
            cachedEntity = null
        }
    }

    private class FakeMapThemeApi(
        val themesToReturn: List<MapThemeModel> = emptyList(),
        val shouldThrow: Boolean = false
    ) : MapThemeApi {
        override suspend fun getMapThemes(): List<MapThemeModel> {
            if (shouldThrow) {
                throw Exception("Network error")
            }
            return themesToReturn
        }
    }

    @Test
    fun getCachedThemes_returnsNullWhenNoCache() = runBlocking {
        val fakeDao = FakeMapThemeCacheDao()
        val repository = MapThemeRepository(fakeDao, FakeMapThemeApi())

        val result = repository.getCachedThemes()
        assertNull(result)
    }

    @Test
    fun getCachedThemes_returnsDecodedListWhenCacheExists() = runBlocking {
        val fakeDao = FakeMapThemeCacheDao()
        val repository = MapThemeRepository(fakeDao, FakeMapThemeApi())
        
        val payload = """[{"id":"theme_1","name":"Theme 1","description":"Desc 1","style":[]}]"""
        fakeDao.saveCache(MapThemeCacheEntity(jsonPayload = payload, lastUpdated = 12345L))

        val result = repository.getCachedThemes()
        assertEquals(1, result?.size)
        assertEquals("theme_1", result?.first()?.id)
        assertEquals("Theme 1", result?.first()?.name)
    }

    @Test
    fun fetchThemesFromNetwork_savesToCacheAndReturnsThemes() = runBlocking {
        val fakeDao = FakeMapThemeCacheDao()
        val theme = MapThemeModel(id = "theme_network", name = "Network Theme", description = "Network Desc", style = emptyList())
        val fakeApi = FakeMapThemeApi(themesToReturn = listOf(theme))
        val repository = MapThemeRepository(fakeDao, fakeApi)

        val result = repository.fetchThemesFromNetwork()
        assertEquals(1, result.size)
        assertEquals("theme_network", result.first().id)

        // Verify it was cached
        val cached = repository.getCachedThemes()
        assertEquals(1, cached?.size)
        assertEquals("theme_network", cached?.first()?.id)
    }
}
