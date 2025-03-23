package com.example.ippt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RunPathDao {
    @Insert
    suspend fun insertPathPoints(points: List<RunPathPoint>)

    @Query("SELECT * FROM run_path_points WHERE runId = :runId ORDER BY id ASC")
    suspend fun getPathPoints(runId: Int): List<RunPathPoint>
}
