package com.andrei.dracones.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM visited_cells")
    suspend fun deleteAll()
}
