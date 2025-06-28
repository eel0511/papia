package com.ai.papia.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "birth_control_records")
data class BirthControlRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date,
    val taken: Boolean,
    val timesTaken: Int = 1,
    val notes: String = ""
)