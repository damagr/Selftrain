package com.selftrain.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(entity = Routine::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("routineId")]
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val completed: Boolean = false,
    val endDate: Long = 0,
    val durationMinutes: Int = 0
)
