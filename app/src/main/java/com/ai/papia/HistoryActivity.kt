package com.ai.papia

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.papia.adapters.HistoryAdapter
import com.ai.papia.adapters.ifaces.OnHistoryItemActionListener
import com.ai.papia.data.BirthControlRecord
import com.ai.papia.data.PeriodRecord
import com.ai.papia.data.Symptom
import com.ai.papia.databinding.ActivityHistoryBinding
import com.ai.papia.room.PeriodTrackerDatabase
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity(),OnHistoryItemActionListener {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var database: PeriodTrackerDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = PeriodTrackerDatabase.getDatabase(this)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        observeData() // 이 줄이 빠져있었습니다!
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "기록 히스토리"
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(this,dateFormat)
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupTabs() {
        binding.chipPeriods.setOnClickListener {
            selectTab("periods")
        }

        binding.chipBirthControl.setOnClickListener {
            selectTab("birth_control")
        }

        binding.chipSymptoms.setOnClickListener {
            selectTab("symptoms")
        }

        // 기본적으로 생리 기록 탭 선택
        selectTab("periods")
    }

    private fun selectTab(tabType: String) {
        // 모든 칩 비활성화
        binding.chipPeriods.isChecked = false
        binding.chipBirthControl.isChecked = false
        binding.chipSymptoms.isChecked = false

        // 선택된 탭 활성화 및 데이터 로드
        when (tabType) {
            "periods" -> {
                binding.chipPeriods.isChecked = true
                loadPeriodHistory()
            }
            "birth_control" -> {
                binding.chipBirthControl.isChecked = true
                loadBirthControlHistory()
            }
            "symptoms" -> {
                binding.chipSymptoms.isChecked = true
                loadSymptomsHistory()
            }
        }
    }

    private fun observeData() {
        viewModel.allPeriods.observe(this) { periods ->
            if (binding.chipPeriods.isChecked) {
                historyAdapter.submitPeriodList(periods)
                updateStatistics(periods)
            }
        }

        viewModel.allBirthControlRecords.observe(this) { records ->
            if (binding.chipBirthControl.isChecked) {
                historyAdapter.submitBirthControlList(records)
                updateBirthControlStatistics(records)
            }
        }

        viewModel.allSymptoms.observe(this) { symptoms ->
            if (binding.chipSymptoms.isChecked) {
                historyAdapter.submitSymptomsList(symptoms)
                updateSymptomsStatistics(symptoms)
            }
        }
    }

    private fun loadPeriodHistory() {
        // Observer가 자동으로 처리하지만, 탭 변경 시 즉시 업데이트
        viewModel.allPeriods.value?.let { periods ->
            historyAdapter.submitPeriodList(periods)
            updateStatistics(periods)
        }
    }

    private fun loadBirthControlHistory() {
        // Observer가 자동으로 처리하지만, 탭 변경 시 즉시 업데이트
        viewModel.allBirthControlRecords.value?.let { records ->
            historyAdapter.submitBirthControlList(records)
            updateBirthControlStatistics(records)
        }
    }

    private fun loadSymptomsHistory() {
        // Observer가 자동으로 처리하지만, 탭 변경 시 즉시 업데이트
        viewModel.allSymptoms.value?.let { symptoms ->
            historyAdapter.submitSymptomsList(symptoms)
            updateSymptomsStatistics(symptoms)
        }
    }

    private fun updateStatistics(periods: List<com.ai.papia.data.PeriodRecord>) {
        if (periods.isEmpty()) {
            binding.tvStatistics.text = "기록된 생리 데이터가 없습니다."
            return
        }

        val completedPeriods = periods.filter { it.endDate != null }
        val totalCycles = completedPeriods.size

        if (totalCycles == 0) {
            binding.tvStatistics.text = "완료된 생리 기록: ${periods.size}개\n진행 중인 생리: ${periods.size - completedPeriods.size}개"
            return
        }

        // 평균 생리 기간 계산
        val averagePeriodLength = completedPeriods.map { period ->
            val startCal = Calendar.getInstance().apply { time = period.startDate }
            val endCal = Calendar.getInstance().apply { time = period.endDate!! }
            ((endCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
        }.average()

        // 평균 주기 길이 계산 (연속된 생리 시작일 간격)
        var totalCycleLength = 0
        var cycleCount = 0

        for (i in 0 until completedPeriods.size - 1) {
            val current = completedPeriods[i]
            val next = completedPeriods[i + 1]

            val currentCal = Calendar.getInstance().apply { time = current.startDate }
            val nextCal = Calendar.getInstance().apply { time = next.startDate }

            val cycleDays = ((currentCal.timeInMillis - nextCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            if (cycleDays > 0) {
                totalCycleLength += cycleDays
                cycleCount++
            }
        }

        val averageCycleLength = if (cycleCount > 0) totalCycleLength.toDouble() / cycleCount else 0.0

        binding.tvStatistics.text = buildString {
            append("총 생리 기록: ${periods.size}개\n")
            append("완료된 주기: ${completedPeriods.size}개\n")
            if (averagePeriodLength > 0) {
                append("평균 생리 기간: ${"%.1f".format(averagePeriodLength)}일\n")
            }
            if (averageCycleLength > 0) {
                append("평균 주기 길이: ${"%.1f".format(averageCycleLength)}일\n")
            }

            // 최근 생리일
            periods.firstOrNull()?.let { lastPeriod ->
                append("최근 생리 시작일: ${dateFormat.format(lastPeriod.startDate)}")
            }
        }
    }

    private fun updateBirthControlStatistics(records: List<com.ai.papia.data.BirthControlRecord>) {
        if (records.isEmpty()) {
            binding.tvStatistics.text = "기록된 피임약 복용 데이터가 없습니다."
            return
        }

        val takenCount = records.count { it.taken }
        val totalRecords = records.size
        val complianceRate = if (totalRecords > 0) (takenCount.toDouble() / totalRecords * 100) else 0.0

        // 최근 7일 복용률
        val calendar = Calendar.getInstance()
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -7)
        }.time

        val recentRecords = records.filter { it.date >= sevenDaysAgo }
        val recentTakenCount = recentRecords.count { it.taken }
        val recentComplianceRate = if (recentRecords.isNotEmpty())
            (recentTakenCount.toDouble() / recentRecords.size * 100) else 0.0

        binding.tvStatistics.text = buildString {
            append("총 기록 일수: ${totalRecords}일\n")
            append("복용한 일수: ${takenCount}일\n")
            append("전체 복용률: ${"%.1f".format(complianceRate)}%\n")
            append("최근 7일 복용률: ${"%.1f".format(recentComplianceRate)}%\n")

            // 최근 복용일
            records.filter { it.taken }.maxByOrNull { it.date }?.let { lastTaken ->
                append("마지막 복용일: ${dateFormat.format(lastTaken.date)}")
            }
        }
    }

    private fun updateSymptomsStatistics(symptoms: List<com.ai.papia.data.Symptom>) {
        if (symptoms.isEmpty()) {
            binding.tvStatistics.text = "기록된 증상 데이터가 없습니다."
            return
        }

        // 증상별 빈도 계산
        val symptomCounts = symptoms.groupBy { it.symptomType }.mapValues { it.value.size }
        val mostCommonSymptom = symptomCounts.maxByOrNull { it.value }

        // 평균 심각도
        val averageSeverity = symptoms.map { it.severity }.average()

        // 최근 30일 증상
        val thirtyDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -30)
        }.time
        val recentSymptoms = symptoms.filter { it.date >= thirtyDaysAgo }

        binding.tvStatistics.text = buildString {
            append("총 증상 기록: ${symptoms.size}개\n")
            append("평균 심각도: ${"%.1f".format(averageSeverity)}/5\n")
            append("최근 30일 기록: ${recentSymptoms.size}개\n")

            mostCommonSymptom?.let { (type, count) ->
                val symptomName = when (type) {
                    com.ai.papia.data.SymptomType.CRAMPS -> "생리통"
                    com.ai.papia.data.SymptomType.HEADACHE -> "두통"
                    com.ai.papia.data.SymptomType.MOOD_SWINGS -> "기분 변화"
                    com.ai.papia.data.SymptomType.BLOATING -> "복부팽만"
                    com.ai.papia.data.SymptomType.ACNE -> "여드름"
                    com.ai.papia.data.SymptomType.FATIGUE -> "피로감"
                    com.ai.papia.data.SymptomType.BREAST_TENDERNESS -> "가슴 압통"
                    com.ai.papia.data.SymptomType.BACK_PAIN -> "허리 통증"
                }
                append("가장 흔한 증상: $symptomName (${count}회)")
            }
        }
    }
    override fun onDeletePeriod(period: PeriodRecord) {
        AlertDialog.Builder(this)
            .setTitle("생리 기록 삭제")
            .setMessage("이 생리 기록을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.deletePeriod(period)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDeleteBirthControl(record: BirthControlRecord) {
        AlertDialog.Builder(this)
            .setTitle("피임약 기록 삭제")
            .setMessage("이 피임약 기록을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.deleteBirthControlRecord(record)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDeleteSymptom(symptom: Symptom) {
        AlertDialog.Builder(this)
            .setTitle("증상 기록 삭제")
            .setMessage("이 증상 기록을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.deleteSymptom(symptom)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}