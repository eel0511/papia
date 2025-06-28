package com.ai.papia.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "period_records")
data class PeriodRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: Date,
    val endDate: Date? = null,
    val flow: PeriodFlow = PeriodFlow.MEDIUM,
    val symptoms: List<String> = emptyList(),
    val notes: String = ""
)