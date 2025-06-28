package com.ai.papia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.papia.adapters.HistoryAdapter
import com.ai.papia.adapters.ifaces.OnHistoryItemActionListener
import com.ai.papia.data.BirthControlRecord
import com.ai.papia.data.PeriodRecord
import com.ai.papia.data.Symptom
import com.ai.papia.data.SymptomType
import com.ai.papia.databinding.FragmentHistoryBinding
import com.ai.papia.room.PeriodTrackerDatabase
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment(), OnHistoryItemActionListener {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: PeriodTrackerDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        database = PeriodTrackerDatabase.getDatabase(requireContext())
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        setupRecyclerView()
        setupTabs()
        observeData()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(this, dateFormat)
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
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
        selectTab("periods") // 기본 탭
    }

    private fun selectTab(tabType: String) {
        binding.chipPeriods.isChecked = false
        binding.chipBirthControl.isChecked = false
        binding.chipSymptoms.isChecked = false

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
        viewModel.allPeriods.observe(viewLifecycleOwner) { periods ->
            if (binding.chipPeriods.isChecked) {
                historyAdapter.submitPeriodList(periods)
                updateStatistics(periods)
            }
        }

        viewModel.allBirthControlRecords.observe(viewLifecycleOwner) { records ->
            if (binding.chipBirthControl.isChecked) {
                historyAdapter.submitBirthControlList(records)
                updateBirthControlStatistics(records)
            }
        }

        viewModel.allSymptoms.observe(viewLifecycleOwner) { symptoms ->
            if (binding.chipSymptoms.isChecked) {
                historyAdapter.submitSymptomsList(symptoms)
                updateSymptomsStatistics(symptoms)
            }
        }
    }

    private fun loadPeriodHistory() {
        viewModel.allPeriods.value?.let {
            historyAdapter.submitPeriodList(it)
            updateStatistics(it)
        }
    }

    private fun loadBirthControlHistory() {
        viewModel.allBirthControlRecords.value?.let {
            historyAdapter.submitBirthControlList(it)
            updateBirthControlStatistics(it)
        }
    }

    private fun loadSymptomsHistory() {
        viewModel.allSymptoms.value?.let {
            historyAdapter.submitSymptomsList(it)
            updateSymptomsStatistics(it)
        }
    }

    private fun updateStatistics(periods: List<PeriodRecord>) {
        if (periods.isEmpty()) {
            binding.tvStatistics.text = "기록된 생리 데이터가 없습니다."
            return
        }

        val completed = periods.filter { it.endDate != null }
        val avgLength = completed.mapNotNull { it.endDate?.let { end ->
            ((end.time - it.startDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
        } }.average()

        val cycleLengths = (0 until completed.size - 1).mapNotNull { i ->
            val curr = completed[i]
            val next = completed[i + 1]
            val diff = ((curr.startDate.time - next.startDate.time) / (1000 * 60 * 60 * 24)).toInt()
            if (diff > 0) diff else null
        }

        val avgCycle = cycleLengths.averageOrNull()

        binding.tvStatistics.text = buildString {
            append("총 생리 기록: ${periods.size}개\n")
            append("완료된 주기: ${completed.size}개\n")
            if (!avgLength.isNaN()) append("평균 생리 기간: %.1f일\n".format(avgLength))
            if (!avgCycle.isNaN()) append("평균 주기 길이: %.1f일\n".format(avgCycle))
            periods.firstOrNull()?.let {
                append("최근 생리 시작일: ${dateFormat.format(it.startDate)}")
            }
        }
    }

    private fun updateBirthControlStatistics(records: List<BirthControlRecord>) {
        if (records.isEmpty()) {
            binding.tvStatistics.text = "기록된 피임약 복용 데이터가 없습니다."
            return
        }

        val takenCount = records.count { it.taken }
        val rate = takenCount.toDouble() / records.size * 100

        val recent7 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -7) }.time
        val recent = records.filter { it.date >= recent7 }
        val recentRate = if (recent.isNotEmpty()) recent.count { it.taken }.toDouble() / recent.size * 100 else 0.0

        binding.tvStatistics.text = buildString {
            append("총 기록 일수: ${records.size}일\n")
            append("복용한 일수: ${takenCount}일\n")
            append("전체 복용률: %.1f%%\n".format(rate))
            append("최근 7일 복용률: %.1f%%\n".format(recentRate))
            records.filter { it.taken }.maxByOrNull { it.date }?.let {
                append("마지막 복용일: ${dateFormat.format(it.date)}")
            }
        }
    }

    private fun updateSymptomsStatistics(symptoms: List<Symptom>) {
        if (symptoms.isEmpty()) {
            binding.tvStatistics.text = "기록된 증상 데이터가 없습니다."
            return
        }

        val counts = symptoms.groupingBy { it.symptomType }.eachCount()
        val common = counts.maxByOrNull { it.value }
        val avgSeverity = symptoms.map { it.severity }.average()

        val recent30 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -30) }.time
        val recent = symptoms.count { it.date >= recent30 }

        binding.tvStatistics.text = buildString {
            append("총 증상 기록: ${symptoms.size}개\n")
            append("평균 심각도: %.1f/5\n".format(avgSeverity))
            append("최근 30일 기록: ${recent}개\n")
            common?.let {
                append("가장 흔한 증상: ${symptomTypeToKorean(it.key)} (${it.value}회)")
            }
        }
    }

    private fun symptomTypeToKorean(type: SymptomType): String = when (type) {
        SymptomType.CRAMPS -> "생리통"
        SymptomType.HEADACHE -> "두통"
        SymptomType.MOOD_SWINGS -> "기분 변화"
        SymptomType.BLOATING -> "복부팽만"
        SymptomType.ACNE -> "여드름"
        SymptomType.FATIGUE -> "피로감"
        SymptomType.BREAST_TENDERNESS -> "가슴 압통"
        SymptomType.BACK_PAIN -> "허리 통증"
    }

    override fun onDeletePeriod(period: PeriodRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("생리 기록 삭제")
            .setMessage("이 생리 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.deletePeriod(period)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDeleteBirthControl(record: BirthControlRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("피임약 기록 삭제")
            .setMessage("이 피임약 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.deleteBirthControlRecord(record)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDeleteSymptom(symptom: Symptom) {
        AlertDialog.Builder(requireContext())
            .setTitle("증상 기록 삭제")
            .setMessage("이 증상 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.deleteSymptom(symptom)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun List<Int>.averageOrNull(): Double {
        return if (this.isNotEmpty()) this.average() else Double.NaN
    }
}
