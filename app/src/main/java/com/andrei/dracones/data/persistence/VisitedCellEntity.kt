package com.andrei.dracones.data.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visited_cells",
    indices = [
        Index(value = ["resolution"]),
        Index(value = ["blockParentH3"]),
        Index(value = ["neighborhoodParentH3"]),
        Index(value = ["districtParentH3"])
    ]
)
data class VisitedCellEntity(
    @PrimaryKey
    val h3Index: String,
    val resolution: Int,
    val blockParentH3: String,
    val neighborhoodParentH3: String,
    val districtParentH3: String,
    val firstVisitedAt: Long,
    val lastVisitedAt: Long,
    val visitCount: Int
)
