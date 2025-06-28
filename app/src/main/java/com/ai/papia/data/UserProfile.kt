package com.ai.papia.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Long = 1,
    val name: String = "",
    val birthDate: Date? = null,
    val height: Float? = null, // cm
    val weight: Float? = null, // kg
    val averageCycleLength: Int = 28, // days
    val averagePeriodLength: Int = 5, // days
    val lastPeriodStartDate: Date? = null,
    val birthControlType: String = "", // 피임약 종류
    val birthControlStartDate: Date? = null
)