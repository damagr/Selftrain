package com.entrenaguay.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,        // "chest", "back", "legs", "shoulders", "arms", "core"
    val category: String,            // "compound" or "isolation"
    val isBilboEligible: Boolean     // true for compound exercises
)
