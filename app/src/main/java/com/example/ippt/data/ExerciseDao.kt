package com.example.ippt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insertExercise(entry: ExerciseEntry): Long

    @Query("SELECT * FROM exercise_entries WHERE type = :type ORDER BY timestamp DESC")
    suspend fun getExercisesByType(type: String): List<ExerciseEntry>

    @Query("DELETE FROM exercise_entries WHERE id = :id")
    suspend fun deleteExercise(id: Int)
}
