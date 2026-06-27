package com.andrei.dracones.data.persistence

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisitedCellDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: VisitedCellDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.visitedCellDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetCell() = runBlocking {
        val cell = VisitedCellEntity(
            h3Index = "8b39014199a0fff",
            resolution = 11,
            blockParentH3 = "parent10",
            neighborhoodParentH3 = "parent9",
            districtParentH3 = "parent8",
            firstVisitedAt = 1000L,
            lastVisitedAt = 1000L,
            visitCount = 1
        )
        dao.insert(cell)
        val allCells = dao.getAllVisitedCells().first()
        assertEquals(1, allCells.size)
        assertEquals("8b39014199a0fff", allCells[0].h3Index)
    }

    @Test
    fun updateCell() = runBlocking {
        val cell = VisitedCellEntity(
            h3Index = "8b39014199a0fff",
            resolution = 11,
            blockParentH3 = "parent10",
            neighborhoodParentH3 = "parent9",
            districtParentH3 = "parent8",
            firstVisitedAt = 1000L,
            lastVisitedAt = 1000L,
            visitCount = 1
        )
        dao.insert(cell)
        
        val fetchedCell = dao.getCellByIndex("8b39014199a0fff")
        assertNotNull(fetchedCell)
        
        val updatedCell = fetchedCell!!.copy(
            lastVisitedAt = 2000L,
            visitCount = 2
        )
        dao.update(updatedCell)
        
        val allCells = dao.getAllVisitedCells().first()
        assertEquals(1, allCells.size)
        assertEquals(2000L, allCells[0].lastVisitedAt)
        assertEquals(2, allCells[0].visitCount)
    }

    @Test
    fun upsertNewCell() = runBlocking {
        dao.upsert(
            h3Index = "h3_1",
            resolution = 11,
            blockParent = "p10",
            neighborhoodParent = "p9",
            districtParent = "p8",
            now = 1000L
        )
        
        val allCells = dao.getAllVisitedCells().first()
        assertEquals(1, allCells.size)
        assertEquals(1, allCells[0].visitCount)
        assertEquals(1000L, allCells[0].firstVisitedAt)
    }

    @Test
    fun upsertExistingCell() = runBlocking {
        dao.upsert("h3_1", 11, "p10", "p9", "p8", 1000L)
        dao.upsert("h3_1", 11, "p10", "p9", "p8", 2000L)
        
        val allCells = dao.getAllVisitedCells().first()
        assertEquals(1, allCells.size)
        assertEquals(2, allCells[0].visitCount)
        assertEquals(1000L, allCells[0].firstVisitedAt)
        assertEquals(2000L, allCells[0].lastVisitedAt)
    }
}
