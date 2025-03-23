package com.example.ippt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_entries")
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "pushup", "situp", "run"
    val timestamp: String, // e.g., "2025-03-24 18:47:13"
    val count: Int? = null, // for pushups & situps
    val summary: String? = null // for runs
)
