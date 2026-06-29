package com.entrenaguay.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val method: String = "bilbo",   // "bilbo" for now, extensible later
    val notes: String = ""
)
