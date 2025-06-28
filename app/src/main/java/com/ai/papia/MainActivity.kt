package com.ai.papia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ai.papia.data.BirthControlRecord
import com.ai.papia.data.PeriodRecord
import com.ai.papia.data.UserProfile

import com.ai.papia.databinding.ActivityMainBinding
import com.ai.papia.decorator.PeriodDecorator
import com.ai.papia.decorator.PreviewPeriodDecorator
import com.ai.papia.decorator.SelectedDayDecorator
import com.ai.papia.decorator.BirthControlDecorator // BirthControlDecorator import 추가
import com.ai.papia.room.PeriodTrackerDatabase
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog


class MainActivity : AppCompatActivity() {
    private var selectedCalendarDay: CalendarDay = CalendarDay.today()
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: PeriodTrackerDatabase
    private val viewModel: MainViewModel by viewModels{
        MainViewModelFactory(database)
    }

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)

    // 선택된 날짜를 위한 데코레이터 인스턴스
    private lateinit var selectedDayDecorator: SelectedDayDecorator

    // PeriodDecorator 인스턴스들을 추적하기 위한 리스트
    private val periodDecoratorsList: MutableList<PeriodDecorator> = mutableListOf()

    // **새로 추가: BirthControlDecorator 인스턴스들을 추적하기 위한 리스트**
    private val birthControlDecoratorsList: MutableList<BirthControlDecorator> = mutableListOf()

    // 생리 기간 정의 로직을 위한 상태 변수
    private var pendingPeriodStartDate: Date? = null      // 생리 시작일이 선택되었지만 아직 종료일이 선택되지 않은 상태

    private var previewPeriodDecorator: PreviewPeriodDecorator? = null // 미리보기 데코레이터 인스턴스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = PeriodTrackerDatabase.getDatabase(this)

        // SelectedDayDecorator 초기화 및 캘린더에 추가
        selectedDayDecorator = SelectedDayDecorator(selectedCalendarDay)
        binding.calendarView.addDecorator(selectedDayDecorator)
        setupBottomNavigation()
        setupToolbar()
        setupCalendar()
        setupButtons()
        observeData()

        // 초기 선택된 날짜 텍스트 설정
        binding.tvSelectedRecordDate.text = "선택된 날짜: ${dateFormat.format(selectedCalendarDay.date)}"
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
            if (selected) {
                // 달력 날짜 클릭 시 처리 로직 유지
                binding.calendarView.removeDecorator(selectedDayDecorator)
                selectedCalendarDay = date
                selectedDayDecorator.setDay(selectedCalendarDay)
                binding.calendarView.addDecorator(selectedDayDecorator)
                binding.tvSelectedRecordDate.text = "선택된 날짜: ${dateFormat.format(selectedCalendarDay.date)}"

                // 달력 클릭 시 생리 기록 로직을 직접 실행하지 않음 (버튼을 통해서만 실행)
            }
        }
    }
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 현재 MainActivity가 홈이므로 아무것도 하지 않음 (또는 홈 화면 초기화 로직)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_symptoms -> {
                    startActivity(Intent(this, SymptomsActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupButtons() {
//        binding.btnSymptoms.setOnClickListener {
//            val intent = Intent(this, SymptomsActivity::class.java)
//            startActivity(intent)
//        }
        binding.btnTakePill.setOnClickListener {
            recordBirthControl()
        }
//        binding.btnHistory.setOnClickListener {
//            val intent = Intent(this, HistoryActivity::class.java)
//            startActivity(intent)
//        }
//        binding.btnProfile.setOnClickListener {
//            val intent = Intent(this, ProfileActivity::class.java)
//            startActivity(intent)
//        }

        binding.btnStartPeriod.setOnClickListener {
            handleButtonStartPeriod()
        }

        binding.btnEndPeriod.setOnClickListener {
            handleButtonEndPeriod()
        }
    }

    private fun observeData() {
        viewModel.allPeriods.observe(this) { periods ->
            Log.d("observeData", "All Periods: $periods")
            updateCalendarDecorators(periods)
        }

        viewModel.userProfile.observe(this) { profile ->
            profile?.let {
                updateCycleInfo(it)
            } ?: run {
                Log.d("MainActivity", "UserProfile is null. Initializing with default values if necessary.")
                lifecycleScope.launch {
                    val currentProfile = database.userProfileDao().getProfileBlocking()
                    if (currentProfile == null) {
                        Log.d("MainActivity", "No existing UserProfile, inserting default.")
                        database.userProfileDao().insertProfile(UserProfile()) // 기본 UserProfile 생성 및 삽입
                    }
                }
            }
        }
        // **새로 추가: 피임약 복용 기록 관찰 및 데코레이터 업데이트**
        viewModel.allBirthControlRecords.observe(this) { records ->
            Log.d("observeData", "All Birth Control Records: $records")
            updateBirthControlDecorators(records.filter { it.taken }) // 복용한 기록만 표시
        }
    }

    private fun getTodayAsMidnight(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun isFutureDate(date: Date): Boolean {
        return date.after(getTodayAsMidnight())
    }

    // handleDateClick 함수는 이제 달력에서 직접 호출되지 않습니다.
    // 버튼 로직에서만 사용됩니다.
    private fun handleDateClick(date: CalendarDay) {
        // 이 함수는 더 이상 달력의 직접적인 클릭 이벤트로 생리 기록을 시작하지 않습니다.
        // 대신, 아래의 handleButtonStartPeriod 및 handleButtonEndPeriod가 이 역할을 대신합니다.
        // 이 함수 자체는 다른 곳에서 재사용될 수 있도록 남겨둡니다.
        // 다만, 현재 시점에서는 캘린더 클릭에서 직접 호출되지 않습니다.
    }


    // **btnStartPeriod 클릭 시 호출될 함수 (선택된 날짜를 시작일로 강제)**
    private fun handleButtonStartPeriod() {
        val selectedJavaDate = Calendar.getInstance().apply {
            set(selectedCalendarDay.year, selectedCalendarDay.month, selectedCalendarDay.day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        if (isFutureDate(selectedJavaDate)) {
            Toast.makeText(this, "미래 날짜는 선택할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val periodOnSelectedDate = database.periodDao().getPeriodByDate(selectedJavaDate)
            if (periodOnSelectedDate != null) {
                // 선택된 날짜에 이미 생리 기록이 있는 경우: 삭제 여부 묻기
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("생리 기록 삭제")
                    .setMessage("${dateFormat.format(periodOnSelectedDate.startDate)} ~ ${dateFormat.format(periodOnSelectedDate.endDate)}의 생리 기록을 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { dialog, which ->
                        deletePeriodRecord(periodOnSelectedDate)
                        pendingPeriodStartDate = null // 삭제 후 시작 대기 상태 초기화
                        removePreviewDecorator()
                        binding.calendarView.invalidateDecorators()
                    }
                    .setNegativeButton("취소") { dialog, which ->
                        pendingPeriodStartDate = null
                        removePreviewDecorator()
                        binding.calendarView.invalidateDecorators()
                    }
                    .show()
                return@launch
            }

            val currentPeriod = database.periodDao().getCurrentPeriod()
            if (currentPeriod != null) {
                Toast.makeText(this@MainActivity, "현재 진행 중인 생리가 있습니다. 종료일을 선택하거나 기존 기록을 삭제해주세요.", Toast.LENGTH_LONG).show()
                return@launch
            }

            pendingPeriodStartDate = selectedJavaDate
            Toast.makeText(this@MainActivity, "생리 시작일: ${dateFormat.format(selectedJavaDate)}", Toast.LENGTH_SHORT).show()

            removePreviewDecorator()
            previewPeriodDecorator = PreviewPeriodDecorator(listOf(selectedCalendarDay), this@MainActivity)
            binding.calendarView.addDecorator(previewPeriodDecorator)
            binding.calendarView.invalidateDecorators()
        }
    }

    // **btnEndPeriod 클릭 시 호출될 함수 (선택된 날짜를 종료일로 강제)**
    private fun handleButtonEndPeriod() {
        val selectedJavaDate = Calendar.getInstance().apply {
            set(selectedCalendarDay.year, selectedCalendarDay.month, selectedCalendarDay.day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        if (isFutureDate(selectedJavaDate)) {
            Toast.makeText(this, "미래 날짜는 선택할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (pendingPeriodStartDate == null) {
            Toast.makeText(this, "생리 시작일을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val periodStartDate = pendingPeriodStartDate!!
        val periodEndDate = selectedJavaDate

        if (periodEndDate.before(periodStartDate)) {
            Toast.makeText(this, "종료일은 시작일보다 이전일 수 없습니다. 다시 선택해주세요.", Toast.LENGTH_LONG).show()
            pendingPeriodStartDate = null
            removePreviewDecorator()
            binding.calendarView.invalidateDecorators()
            return
        }

        // 미리보기 데코레이터 업데이트 (시작일과 종료일 범위 표시)
        removePreviewDecorator() // 기존 미리보기 제거
        val previewDays = getDaysBetween(periodStartDate, periodEndDate)
        previewPeriodDecorator = PreviewPeriodDecorator(previewDays.map { CalendarDay.from(it) }, this@MainActivity)
        binding.calendarView.addDecorator(previewPeriodDecorator)

        // 생리 기록 저장 전 확인 알림창
        AlertDialog.Builder(this)
            .setTitle("생리 기록 저장")
            .setMessage("${dateFormat.format(periodStartDate)} ~ ${dateFormat.format(periodEndDate)}까지의 생리 기록을 저장하시겠습니까?")
            .setPositiveButton("저장") { dialog, which ->
                lifecycleScope.launch {
                    val newPeriod = PeriodRecord(
                        startDate = periodStartDate,
                        endDate = periodEndDate
                    )
                    database.periodDao().insertPeriod(newPeriod)
                    Toast.makeText(this@MainActivity, "생리 기간 기록 완료: ${dateFormat.format(periodStartDate)} ~ ${dateFormat.format(periodEndDate)}", Toast.LENGTH_LONG).show()

                    pendingPeriodStartDate = null
                    removePreviewDecorator()

                    viewModel.calculateAndSaveAverageCycleLength()
                    binding.calendarView.invalidateDecorators()
                }
            }
            .setNegativeButton("취소") { dialog, which ->
                pendingPeriodStartDate = null
                removePreviewDecorator()
                binding.calendarView.invalidateDecorators()
                Toast.makeText(this@MainActivity, "생리 기록 저장이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }


    private fun deletePeriodRecord(periodRecord: PeriodRecord) {
        lifecycleScope.launch {
            try {
                database.periodDao().deletePeriod(periodRecord)
                Toast.makeText(this@MainActivity, "생리 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                viewModel.allPeriods.value?.let { updateCalendarDecorators(it) }
                viewModel.calculateAndSaveAverageCycleLength()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "기록 삭제 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error deleting period record", e)
            }
        }
    }

    private fun removePreviewDecorator() {
        previewPeriodDecorator?.let {
            binding.calendarView.removeDecorator(it)
        }
        previewPeriodDecorator = null
    }


    private fun recordBirthControl() {
        lifecycleScope.launch {
            try {
                val recordDate = Calendar.getInstance().apply {
                    set(selectedCalendarDay.year, selectedCalendarDay.month, selectedCalendarDay.day)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val existingRecord = database.birthControlDao().getRecordForDate(recordDate)
                if (existingRecord != null) {
                    val updatedRecord = existingRecord.copy(taken = !existingRecord.taken)
                    database.birthControlDao().updateRecord(updatedRecord)
                    if (updatedRecord.taken) {
                        Toast.makeText(this@MainActivity, "${dateFormat.format(recordDate)} 피임약 복용이 기록되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "${dateFormat.format(recordDate)} 피임약 복용 기록이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val newRecord = BirthControlRecord(date = recordDate, taken = true)
                    database.birthControlDao().insertRecord(newRecord)
                    Toast.makeText(this@MainActivity, "${dateFormat.format(recordDate)} 피임약 복용이 기록되었습니다.", Toast.LENGTH_SHORT).show()
                }
                // 피임약 복용 기록 변경 시 데코레이터 업데이트
                viewModel.allBirthControlRecords.value?.let { updateBirthControlDecorators(it.filter { it.taken }) }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "기록 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error recording birth control", e)
            }
        }
    }

    private fun updateCalendarDecorators(periods: List<PeriodRecord>) {
        periodDecoratorsList.forEach {
            binding.calendarView.removeDecorator(it)
        }
        periodDecoratorsList.clear()

        periods.forEach { period ->
            if (period.endDate != null) {
                val startCalendar = Calendar.getInstance().apply {
                    time = period.startDate
                }
                val endCalendar = Calendar.getInstance().apply {
                    time = period.endDate
                }

                val periodDays = mutableListOf<CalendarDay>()
                val currentCalendar = startCalendar.clone() as Calendar

                while (currentCalendar.timeInMillis <= endCalendar.timeInMillis) {
                    periodDays.add(CalendarDay.from(
                        currentCalendar.get(Calendar.YEAR),
                        currentCalendar.get(Calendar.MONTH),
                        currentCalendar.get(Calendar.DAY_OF_MONTH)
                    ))
                    currentCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                if (periodDays.isNotEmpty()) {
                    val newPeriodDecorator = PeriodDecorator(periodDays, this)
                    binding.calendarView.addDecorator(newPeriodDecorator)
                    periodDecoratorsList.add(newPeriodDecorator)
                }
            }
        }
        binding.calendarView.invalidateDecorators()
    }

    // **새로 추가: 피임약 복용 기록 데코레이터 업데이트 함수**
    private fun updateBirthControlDecorators(records: List<BirthControlRecord>) {
        // 기존 피임약 데코레이터 제거
        birthControlDecoratorsList.forEach {
            binding.calendarView.removeDecorator(it)
        }
        birthControlDecoratorsList.clear()

        // 새로운 피임약 데코레이터 추가
        val birthControlDays = records.map {
            CalendarDay.from(it.date)
        }.toSet() // 중복 날짜 방지를 위해 Set으로 변환

        if (birthControlDays.isNotEmpty()) {
            val newBirthControlDecorator = BirthControlDecorator(birthControlDays)
            binding.calendarView.addDecorator(newBirthControlDecorator)
            birthControlDecoratorsList.add(newBirthControlDecorator)
        }
        binding.calendarView.invalidateDecorators() // 데코레이터 변경사항 적용
    }


    private fun updateCycleInfo(profile: UserProfile) {
        profile.lastPeriodStartDate?.let { lastPeriodDate ->
            Log.d("updateCycleInfo",""+lastPeriodDate)
            val calendar = Calendar.getInstance()
            val today = getTodayAsMidnight()

            if (lastPeriodDate.after(today)) {
                binding.tvNextPeriodDate.text = "다음 생리 예정일: 날짜 오류"
                binding.tvCycleDay.text = "현재 주기: 날짜 오류"
                binding.tvAverageCycleLength.text = "평균 주기 길이: 기록 부족"
                Log.e("MainActivity", "Last Period Start Date (${dateFormat.format(lastPeriodDate)}) is in the future. Displaying error.")
                return
            }

            calendar.time = lastPeriodDate

            val effectiveAverageCycleLength = if (profile.averageCycleLength <= 0) {
                28
            } else {
                profile.averageCycleLength
            }

            calendar.add(Calendar.DAY_OF_MONTH, effectiveAverageCycleLength)
            val nextPeriodDate = calendar.time

            binding.tvNextPeriodDate.text = "다음 생리 예정일: ${dateFormat.format(nextPeriodDate)}"
            Log.d("MainActivity", "다음 생리 예정일 계산됨 (사용된 주기 길이: $effectiveAverageCycleLength 일): ${dateFormat.format(nextPeriodDate)}")

            val daysDiff = ((today.time - lastPeriodDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
            binding.tvCycleDay.text = "현재 주기 ${daysDiff}일째"
            Log.d("MainActivity", "현재 주기 일수 계산됨: ${daysDiff}일")

            if (profile.averageCycleLength > 0) {
                binding.tvAverageCycleLength.text = "평균 주기 길이: ${profile.averageCycleLength} 일"
            } else {
                binding.tvAverageCycleLength.text = "평균 주기 길이: 기록 부족"
            }
        } ?: run {
            binding.tvNextPeriodDate.text = "다음 생리 예정일: 기록 없음"
            binding.tvCycleDay.text = "주기 일: 기록 없음"
            binding.tvAverageCycleLength.text = "평균 주기 길이: 기록 없음"
        }
    }

    // 두 날짜 사이의 모든 CalendarDay를 반환하는 헬퍼 함수
    private fun getDaysBetween(startDate: Date, endDate: Date): List<Date> {
        val days = mutableListOf<Date>()
        val calendar = Calendar.getInstance().apply { time = startDate }
        val endCalendar = Calendar.getInstance().apply { time = endDate }

        while (calendar.timeInMillis <= endCalendar.timeInMillis) {
            days.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return days
    }
}