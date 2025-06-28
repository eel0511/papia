package com.ai.papia.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "symptoms")
data class Symptom(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date,
    val symptomType: SymptomType,
    val severity: Int, // 1-5 scale
    val notes: String = ""
)