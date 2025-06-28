package com.ai.papia.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ai.papia.data.Symptom
import java.util.Date

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptoms WHERE date = :date")
    suspend fun getSymptomsForDate(date: Date): List<Symptom>

    @Query("SELECT * FROM symptoms ORDER BY date DESC")
    fun getAllSymptoms(): LiveData<List<Symptom>>

    @Insert
    suspend fun insertSymptom(symptom: Symptom)

    @Delete
    suspend fun deleteSymptom(symptom: Symptom)
}