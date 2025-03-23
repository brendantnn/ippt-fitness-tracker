package com.example.ippt.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_path_points",
    foreignKeys = [ForeignKey(
        entity = ExerciseEntry::class,
        parentColumns = ["id"],
        childColumns = ["runId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class RunPathPoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val runId: Int,
    val latitude: Double,
    val longitude: Double
)
