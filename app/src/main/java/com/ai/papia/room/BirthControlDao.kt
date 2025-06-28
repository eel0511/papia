package com.ai.papia.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ai.papia.data.BirthControlRecord
import java.util.Date


@Dao
interface BirthControlDao {
    @Query("SELECT * FROM birth_control_records WHERE date = :date")
    suspend fun getRecordForDate(date: Date): BirthControlRecord?

    @Query("SELECT * FROM birth_control_records ORDER BY date DESC")
    fun getAllRecords(): LiveData<List<BirthControlRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BirthControlRecord)

    @Update
    suspend fun updateRecord(record: BirthControlRecord)

    @Query("SELECT COUNT(*) FROM birth_control_records WHERE date BETWEEN :startDate AND :endDate AND taken = 1")
    suspend fun getTakenCountInRange(startDate: Date, endDate: Date): Int

    @Delete
    suspend fun deleteBirthControlRecord(record: BirthControlRecord)
}