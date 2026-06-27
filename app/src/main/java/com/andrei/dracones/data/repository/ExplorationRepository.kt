package com.andrei.dracones.data.repository

import com.andrei.dracones.data.persistence.VisitedCellDao
import com.andrei.dracones.data.persistence.VisitedCellEntity
import com.andrei.dracones.domain.h3.H3Manager
import kotlinx.coroutines.flow.Flow

class ExplorationRepository(private val visitedCellDao: VisitedCellDao) {
    
    val allVisitedCells: Flow<List<VisitedCellEntity>> = visitedCellDao.getAllVisitedCells()

    suspend fun markCellVisited(h3Index: String) {
        val existingCell = visitedCellDao.getCellByIndex(h3Index)
        val now = System.currentTimeMillis()
        
        if (existingCell == null) {
            val newCell = VisitedCellEntity(
                h3Index = h3Index,
                resolution = H3Manager.EXPLORATION_RESOLUTION,
                blockParentH3 = H3Manager.getParent(h3Index, H3Manager.BLOCK_PARENT_RESOLUTION),
                neighborhoodParentH3 = H3Manager.getParent(h3Index, H3Manager.NEIGHBORHOOD_PARENT_RESOLUTION),
                districtParentH3 = H3Manager.getParent(h3Index, H3Manager.DISTRICT_PARENT_RESOLUTION),
                firstVisitedAt = now,
                lastVisitedAt = now,
                visitCount = 1
            )
            visitedCellDao.insert(newCell)
        } else {
            val updatedCell = existingCell.copy(
                lastVisitedAt = now,
                visitCount = existingCell.visitCount + 1
            )
            visitedCellDao.update(updatedCell)
        }
    }

    suspend fun clearAll() {
        visitedCellDao.deleteAll()
    }
}
