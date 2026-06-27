package com.andrei.dracones.data.repository

import android.util.Log
import com.andrei.dracones.data.persistence.VisitedCellDao
import com.andrei.dracones.data.persistence.VisitedCellEntity
import com.andrei.dracones.domain.h3.H3Manager
import kotlinx.coroutines.flow.Flow

class ExplorationRepository(private val visitedCellDao: VisitedCellDao) {
    
    companion object {
        private const val TAG = "HSD"
    }
    
    val allVisitedCells: Flow<List<VisitedCellEntity>> = visitedCellDao.getAllVisitedCells()

    suspend fun markCellVisited(h3Index: String) {
        val now = System.currentTimeMillis()
        
        // Use DAO transaction for atomic upsert
        visitedCellDao.upsert(
            h3Index = h3Index,
            resolution = H3Manager.EXPLORATION_RESOLUTION,
            blockParent = H3Manager.getParent(h3Index, H3Manager.BLOCK_PARENT_RESOLUTION),
            neighborhoodParent = H3Manager.getParent(h3Index, H3Manager.NEIGHBORHOOD_PARENT_RESOLUTION),
            districtParent = H3Manager.getParent(h3Index, H3Manager.DISTRICT_PARENT_RESOLUTION),
            now = now
        )
        
        Log.d(TAG, "Cell visited: $h3Index at $now")
    }

    suspend fun clearAll() {
        Log.d(TAG, "Clearing all visited cells")
        visitedCellDao.deleteAll()
    }
}
