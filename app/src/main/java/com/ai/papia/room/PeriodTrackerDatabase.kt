package com.ai.papia.room


import  com.ai.papia.data.PeriodRecord
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ai.papia.data.BirthControlRecord
import com.ai.papia.data.Symptom
import com.ai.papia.data.UserProfile
import com.ai.papia.data.database.Converters


// Database
@Database(
    entities = [PeriodRecord::class, BirthControlRecord::class, UserProfile::class, Symptom::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PeriodTrackerDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun birthControlDao(): BirthControlDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun symptomDao(): SymptomDao

    companion object {
        @Volatile
        private var INSTANCE: PeriodTrackerDatabase? = null

        fun getDatabase(context: android.content.Context): PeriodTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PeriodTrackerDatabase::class.java,
                    "period_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}