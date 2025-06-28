package com.ai.papia.room


import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ai.papia.data.PeriodRecord
import java.util.Date

@Dao
interface PeriodDao {
    @Query("SELECT * FROM period_records ORDER BY startDate DESC")
    fun getAllPeriods(): LiveData<List<PeriodRecord>>

    @Query("SELECT * FROM period_records WHERE startDate BETWEEN :startDate AND :endDate")
    fun getPeriodsInRange(startDate: Date, endDate: Date): LiveData<List<PeriodRecord>>

    @Insert
    suspend fun insertPeriod(period: PeriodRecord)

    @Update
    suspend fun updatePeriod(period: PeriodRecord)

    @Delete
    suspend fun deletePeriod(period: PeriodRecord)

    @Query("SELECT * FROM period_records WHERE endDate IS NULL ORDER BY startDate DESC LIMIT 1")
    suspend fun getCurrentPeriod(): PeriodRecord?

    @Query("DELETE FROM period_records")
    suspend fun deleteAllPeriods()

    // **새로 추가된 함수**
    @Query("SELECT * FROM period_records WHERE endDate IS NOT NULL ORDER BY startDate ASC")
    suspend fun getAllCompletedPeriodsSortedByStartDate(): List<PeriodRecord>

    // 특정 날짜에 대한 생리 기록을 가져오는 쿼리를 추가합니다.
    @Query("SELECT * FROM period_records WHERE :date BETWEEN startDate AND endDate LIMIT 1")
    suspend fun getPeriodByDate(date: Date): PeriodRecord?

    @Query("SELECT * FROM period_records ORDER BY startDate ASC")
    suspend fun getAllPeriodsOrderedByStartDate(): List<PeriodRecord>
}