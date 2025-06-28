// HomeFragment.kt
package com.ai.papia

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ai.papia.data.BirthControlRecord
import com.ai.papia.data.PeriodRecord
import com.ai.papia.data.UserProfile
import com.ai.papia.databinding.ActivityMainBinding
import com.ai.papia.databinding.FragmentHomeBinding // We will create this new binding
import com.ai.papia.decorator.BirthControlDecorator
import com.ai.papia.decorator.PeriodDecorator
import com.ai.papia.decorator.PreviewPeriodDecorator
import com.ai.papia.decorator.SelectedDayDecorator
import com.ai.papia.room.PeriodTrackerDatabase
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var selectedCalendarDay: CalendarDay = CalendarDay.today()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: PeriodTrackerDatabase
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)

    // 선택된 날짜를 위한 데코레이터 인스턴스
    private lateinit var selectedDayDecorator: SelectedDayDecorator

    // PeriodDecorator 인스턴스들을 추적하기 위한 리스트
    private val periodDecoratorsList: MutableList<PeriodDecorator> = mutableListOf()

    // BirthControlDecorator 인스턴스들을 추적하기 위한 리스트
    private val birthControlDecoratorsList: MutableList<BirthControlDecorator> = mutableListOf()

    // 생리 기간 정의 로직을 위한 상태 변수
    private var pendingPeriodStartDate: Date? = null

    private var previewPeriodDecorator: PreviewPeriodDecorator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = PeriodTrackerDatabase.getDatabase(requireContext())

        selectedDayDecorator = SelectedDayDecorator(selectedCalendarDay)
        binding.calendarView.addDecorator(selectedDayDecorator)

        setupCalendar()
        setupButtons()
        observeData()

        binding.tvSelectedRecordDate.text = "선택된 날짜: ${dateFormat.format(selectedCalendarDay.date)}"
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
            if (selected) {
                binding.calendarView.removeDecorator(selectedDayDecorator)
                selectedCalendarDay = date
                selectedDayDecorator.setDay(selectedCalendarDay)
                binding.calendarView.addDecorator(selectedDayDecorator)
                binding.tvSelectedRecordDate.text =
                    "선택된 날짜: ${dateFormat.format(selectedCalendarDay.date)}"
            }
        }
    }

    private fun setupButtons() {
        binding.btnTakePill.setOnClickListener {
            recordBirthControl()
        }

        binding.btnStartPeriod.setOnClickListener {
            handleButtonStartPeriod()
        }

        binding.btnEndPeriod.setOnClickListener {
            handleButtonEndPeriod()
        }
    }

    private fun observeData() {
        viewModel.allPeriods.observe(viewLifecycleOwner) { periods ->
            Log.d("HomeFragment", "All Periods: $periods")
            updateCalendarDecorators(periods)
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                updateCycleInfo(it)
            } ?: run {
                Log.d(
                    "HomeFragment",
                    "UserProfile is null. Initializing with default values if necessary."
                )
                lifecycleScope.launch {
                    val currentProfile = database.userProfileDao().getProfileBlocking()
                    if (currentProfile == null) {
                        Log.d("HomeFragment", "No existing UserProfile, inserting default.")
                        database.userProfileDao()
                            .insertProfile(UserProfile())
                    }
                }
            }
        }

        viewModel.allBirthControlRecords.observe(viewLifecycleOwner) { records ->
            Log.d("HomeFragment", "All Birth Control Records: $records")
            updateBirthControlDecorators(records.filter { it.taken })
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

    private fun handleButtonStartPeriod() {
        val selectedJavaDate = Calendar.getInstance().apply {
            set(selectedCalendarDay.year, selectedCalendarDay.month, selectedCalendarDay.day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        if (isFutureDate(selectedJavaDate)) {
            Toast.makeText(requireContext(), "미래 날짜는 선택할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val periodOnSelectedDate = database.periodDao().getPeriodByDate(selectedJavaDate)
            if (periodOnSelectedDate != null) {
                AlertDialog.Builder(requireContext())
                    .setTitle("생리 기록 삭제")
                    .setMessage(
                        "${dateFormat.format(periodOnSelectedDate.startDate)} ~ ${
                            dateFormat.format(
                                periodOnSelectedDate.endDate
                            )
                        }의 생리 기록을 삭제하시겠습니까?"
                    )
                    .setPositiveButton("삭제") { dialog, which ->
                        deletePeriodRecord(periodOnSelectedDate)
                        pendingPeriodStartDate = null
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
                Toast.makeText(
                    requireContext(),
                    "현재 진행 중인 생리가 있습니다. 종료일을 선택하거나 기존 기록을 삭제해주세요.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            pendingPeriodStartDate = selectedJavaDate
            Toast.makeText(
                requireContext(),
                "생리 시작일: ${dateFormat.format(selectedJavaDate)}",
                Toast.LENGTH_SHORT
            ).show()

            removePreviewDecorator()
            previewPeriodDecorator =
                PreviewPeriodDecorator(listOf(selectedCalendarDay), requireContext())
            binding.calendarView.addDecorator(previewPeriodDecorator)
            binding.calendarView.invalidateDecorators()
        }
    }

    private fun handleButtonEndPeriod() {
        val selectedJavaDate = Calendar.getInstance().apply {
            set(selectedCalendarDay.year, selectedCalendarDay.month, selectedCalendarDay.day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        if (isFutureDate(selectedJavaDate)) {
            Toast.makeText(requireContext(), "미래 날짜는 선택할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (pendingPeriodStartDate == null) {
            Toast.makeText(requireContext(), "생리 시작일을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val periodStartDate = pendingPeriodStartDate!!
        val periodEndDate = selectedJavaDate

        if (periodEndDate.before(periodStartDate)) {
            Toast.makeText(requireContext(), "종료일은 시작일보다 이전일 수 없습니다. 다시 선택해주세요.", Toast.LENGTH_LONG).show()
            pendingPeriodStartDate = null
            removePreviewDecorator()
            binding.calendarView.invalidateDecorators()
            return
        }

        removePreviewDecorator()
        val previewDays = getDaysBetween(periodStartDate, periodEndDate)
        previewPeriodDecorator =
            PreviewPeriodDecorator(previewDays.map { CalendarDay.from(it) }, requireContext())
        binding.calendarView.addDecorator(previewPeriodDecorator)

        AlertDialog.Builder(requireContext())
            .setTitle("생리 기록 저장")
            .setMessage("${dateFormat.format(periodStartDate)} ~ ${dateFormat.format(periodEndDate)}까지의 생리 기록을 저장하시겠습니까?")
            .setPositiveButton("저장") { dialog, which ->
                lifecycleScope.launch {
                    val newPeriod = PeriodRecord(
                        startDate = periodStartDate,
                        endDate = periodEndDate
                    )
                    database.periodDao().insertPeriod(newPeriod)
                    Toast.makeText(
                        requireContext(),
                        "생리 기간 기록 완료: ${dateFormat.format(periodStartDate)} ~ ${
                            dateFormat.format(periodEndDate)
                        }",
                        Toast.LENGTH_LONG
                    ).show()

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
                Toast.makeText(requireContext(), "생리 기록 저장이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun deletePeriodRecord(periodRecord: PeriodRecord) {
        lifecycleScope.launch {
            try {
                database.periodDao().deletePeriod(periodRecord)
                Toast.makeText(requireContext(), "생리 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                viewModel.allPeriods.value?.let { updateCalendarDecorators(it) }
                viewModel.calculateAndSaveAverageCycleLength()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "기록 삭제 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("HomeFragment", "Error deleting period record", e)
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
                    set(
                        selectedCalendarDay.year,
                        selectedCalendarDay.month,
                        selectedCalendarDay.day
                    )
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
                        Toast.makeText(
                            requireContext(),
                            "${dateFormat.format(recordDate)} 피임약 복용이 기록되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "${dateFormat.format(recordDate)} 피임약 복용 기록이 취소되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val newRecord = BirthControlRecord(date = recordDate, taken = true)
                    database.birthControlDao().insertRecord(newRecord)
                    Toast.makeText(
                        requireContext(),
                        "${dateFormat.format(recordDate)} 피임약 복용이 기록되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                viewModel.allBirthControlRecords.value?.let { updateBirthControlDecorators(it.filter { it.taken }) }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "기록 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("HomeFragment", "Error recording birth control", e)
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
                    periodDays.add(
                        CalendarDay.from(
                            currentCalendar.get(Calendar.YEAR),
                            currentCalendar.get(Calendar.MONTH),
                            currentCalendar.get(Calendar.DAY_OF_MONTH)
                        )
                    )
                    currentCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                if (periodDays.isNotEmpty()) {
                    val newPeriodDecorator = PeriodDecorator(periodDays, requireContext())
                    binding.calendarView.addDecorator(newPeriodDecorator)
                    periodDecoratorsList.add(newPeriodDecorator)
                }
            }
        }
        binding.calendarView.invalidateDecorators()
    }

    private fun updateBirthControlDecorators(records: List<BirthControlRecord>) {
        birthControlDecoratorsList.forEach {
            binding.calendarView.removeDecorator(it)
        }
        birthControlDecoratorsList.clear()

        val birthControlDays = records.map {
            CalendarDay.from(it.date)
        }.toSet()

        if (birthControlDays.isNotEmpty()) {
            val newBirthControlDecorator = BirthControlDecorator(birthControlDays)
            binding.calendarView.addDecorator(newBirthControlDecorator)
            birthControlDecoratorsList.add(newBirthControlDecorator)
        }
        binding.calendarView.invalidateDecorators()
    }

    private fun updateCycleInfo(profile: UserProfile) {
        profile.lastPeriodStartDate?.let { lastPeriodDate ->
            Log.d("HomeFragment", "" + lastPeriodDate)
            val calendar = Calendar.getInstance()
            val today = getTodayAsMidnight()

            if (lastPeriodDate.after(today)) {
                binding.tvNextPeriodDate.text = "다음 생리 예정일: 날짜 오류"
                binding.tvCycleDay.text = "현재 주기: 날짜 오류"
                binding.tvAverageCycleLength.text = "평균 주기 길이: 기록 부족"
                Log.e(
                    "HomeFragment",
                    "Last Period Start Date (${dateFormat.format(lastPeriodDate)}) is in the future. Displaying error."
                )
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
            Log.d(
                "HomeFragment",
                "다음 생리 예정일 계산됨 (사용된 주기 길이: $effectiveAverageCycleLength 일): ${
                    dateFormat.format(nextPeriodDate)
                }"
            )

            val daysDiff = ((today.time - lastPeriodDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
            binding.tvCycleDay.text = "현재 주기 ${daysDiff}일째"
            Log.d("HomeFragment", "현재 주기 일수 계산됨: ${daysDiff}일")

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}