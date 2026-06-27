package com.andrei.dracones.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitedCellDao {
    @Query("SELECT * FROM visited_cells")
    fun getAllVisitedCells(): Flow<List<VisitedCellEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cell: VisitedCellEntity): Long

    @Update
    suspend fun update(cell: VisitedCellEntity)

    @Query("SELECT * FROM visited_cells WHERE h3Index = :h3Index")
    suspend fun getCellByIndex(h3Index: String): VisitedCellEntity?

    @Transaction
    suspend fun upsert(
        h3Index: String,
        resolution: Int,
        blockParent: String,
        neighborhoodParent: String,
        districtParent: String,
        now: Long
    ) {
        val existing = getCellByIndex(h3Index)
        if (existing == null) {
            insert(
                VisitedCellEntity(
                    h3Index = h3Index,
                    resolution = resolution,
                    blockParentH3 = blockParent,
                    neighborhoodParentH3 = neighborhoodParent,
                    districtParentH3 = districtParent,
                    firstVisitedAt = now,
                    lastVisitedAt = now,
                    visitCount = 1
                )
            )
        } else {
            update(
                existing.copy(
                    lastVisitedAt = now,
                    visitCount = existing.visitCount + 1
                )
            )
        }
    }

    @Query("DELETE FROM visited_cells")
    suspend fun deleteAll()
}
