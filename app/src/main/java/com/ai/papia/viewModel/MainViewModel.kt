package com.ai.papia.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ai.papia.data.BirthControlRecord
import com.ai.papia.data.PeriodRecord
import com.ai.papia.data.Symptom
import com.ai.papia.data.UserProfile
import com.ai.papia.room.PeriodTrackerDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MainViewModel(private val database: PeriodTrackerDatabase) : ViewModel() {

    val allPeriods: LiveData<List<PeriodRecord>> = database.periodDao().getAllPeriods()
    val userProfile: LiveData<UserProfile> = database.userProfileDao().getUserProfile()
    val allBirthControlRecords: LiveData<List<BirthControlRecord>> = database.birthControlDao().getAllRecords()
    val allSymptoms: LiveData<List<Symptom>> = database.symptomDao().getAllSymptoms()

    fun insertPeriod(period: PeriodRecord) {
        viewModelScope.launch {
            database.periodDao().insertPeriod(period)
        }
    }

    fun updatePeriod(period: PeriodRecord) {
        viewModelScope.launch {
            database.periodDao().updatePeriod(period)
        }
    }

    fun deletePeriod(period: PeriodRecord) {
        viewModelScope.launch {
            database.periodDao().deletePeriod(period)
        }
    }

    fun insertBirthControlRecord(record: BirthControlRecord) {
        viewModelScope.launch {
            database.birthControlDao().insertRecord(record)
        }
    }

    fun deleteBirthControlRecord(record: BirthControlRecord) {
        viewModelScope.launch {
            database.birthControlDao().deleteBirthControlRecord(record)
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            database.userProfileDao().updateProfile(profile)
        }
    }

    fun insertSymptom(symptom: Symptom) {
        viewModelScope.launch {
            database.symptomDao().insertSymptom(symptom)
        }
    }

    fun deleteSymptom(symptom: Symptom){
        viewModelScope.launch {
            database.symptomDao().deleteSymptom(symptom)
        }
    }

    suspend fun getCurrentPeriod(): PeriodRecord? {
        return database.periodDao().getCurrentPeriod()
    }

    suspend fun getBirthControlForDate(date: Date): BirthControlRecord? {
        return database.birthControlDao().getRecordForDate(date)
    }

    suspend fun getSymptomsForDate(date: Date): List<Symptom> {
        return database.symptomDao().getSymptomsForDate(date)
    }

    fun calculateAndSaveAverageCycleLength() {
        viewModelScope.launch {
            try {
                val periods = database.periodDao().getAllCompletedPeriodsSortedByStartDate()
                Log.d("MainViewModel", "Fetched completed periods for cycle calculation: ${periods.size} records")

                val currentUserProfile = database.userProfileDao().getProfileBlocking()

                // 여기에 기존의 UserProfile().averageCycleLength 기본값을 명시적으로 가져옵니다.
                val defaultAverageCycleLength = 28 // UserProfile 데이터 클래스의 기본값

                if (periods.size < 2) {
                    Log.d("MainViewModel", "평균 생리 주기를 계산하기에 완료된 생리 기록이 충분하지 않습니다 (최소 2개 필요).")
                    // 데이터가 부족하므로 DB에는 averageCycleLength를 0으로 저장
                    // UI에서 0일 경우 '기록 부족'으로 표시하고, 예정일 계산 시에는 기본값(28일)을 사용하도록 MainActivity에서 처리
                    val updatedProfile = currentUserProfile?.copy(
                        averageCycleLength = 0, // DB에는 0으로 저장
                        lastPeriodStartDate = periods.lastOrNull()?.startDate ?: currentUserProfile?.lastPeriodStartDate
                    ) ?: UserProfile(averageCycleLength = 0, lastPeriodStartDate = periods.lastOrNull()?.startDate) // 프로필 없는 경우

                    database.userProfileDao().insertProfile(updatedProfile) // insertProfile은 REPLACE 전략이므로 upsert처럼 작동
                    return@launch
                }

                val cycleLengths = mutableListOf<Long>()
                for (i in 1 until periods.size) {
                    val previousPeriod = periods[i - 1]
                    val currentPeriod = periods[i]

                    val diffInMillis = currentPeriod.startDate.time - previousPeriod.startDate.time
                    val diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)

                    if (diffInDays > 0) {
                        cycleLengths.add(diffInDays)
                        Log.d("MainViewModel", "Calculated cycle length from ${dateFormat.format(previousPeriod.startDate)} to ${dateFormat.format(currentPeriod.startDate)}: ${diffInDays} days")
                    } else {
                        Log.w("MainViewModel", "Skipping invalid cycle length: ${diffInDays} days for periods starting ${dateFormat.format(previousPeriod.startDate)} and ${dateFormat.format(currentPeriod.startDate)}")
                    }
                }

                if (cycleLengths.isNotEmpty()) {
                    val averageCycleLength = cycleLengths.average().roundToInt()

                    val updatedProfile = currentUserProfile?.copy(
                        averageCycleLength = averageCycleLength, // 실제 계산된 평균 주기 저장
                        lastPeriodStartDate = periods.lastOrNull()?.startDate ?: currentUserProfile.lastPeriodStartDate
                    ) ?: UserProfile(averageCycleLength = averageCycleLength, lastPeriodStartDate = periods.lastOrNull()?.startDate)

                    database.userProfileDao().insertProfile(updatedProfile)
                    Log.d("MainViewModel", "평균 생리 주기 길이가 업데이트되었습니다: $averageCycleLength 일. 마지막 생리 시작일: ${dateFormat.format(updatedProfile.lastPeriodStartDate)}")
                } else {
                    Log.d("MainViewModel", "계산된 유효한 생리 주기가 없어 평균을 낼 수 없습니다. averageCycleLength를 0으로 설정.")
                    val updatedProfile = currentUserProfile?.copy(
                        averageCycleLength = 0, // 유효한 주기가 없을 때 0으로 설정
                        lastPeriodStartDate = periods.lastOrNull()?.startDate ?: currentUserProfile.lastPeriodStartDate
                    ) ?: UserProfile(averageCycleLength = 0, lastPeriodStartDate = periods.lastOrNull()?.startDate)

                    database.userProfileDao().insertProfile(updatedProfile)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "평균 생리 주기 계산 중 오류 발생: ${e.message}", e)
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
}

class MainViewModelFactory(private val database: PeriodTrackerDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}