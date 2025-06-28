package com.ai.papia

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.papia.R
import com.ai.papia.adapters.SymptomsAdapter
import com.ai.papia.data.Symptom
import com.ai.papia.data.SymptomType
import com.ai.papia.databinding.ActivitySymptomsBinding
import com.ai.papia.room.PeriodTrackerDatabase
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SymptomsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySymptomsBinding
    private lateinit var database: PeriodTrackerDatabase
    private lateinit var symptomsAdapter: SymptomsAdapter
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    private var selectedDate = Calendar.getInstance().time

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySymptomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = PeriodTrackerDatabase.getDatabase(this)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        setupDatePicker()
        observeData()
        loadTodaysSymptoms()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "증상 기록"
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        symptomsAdapter = SymptomsAdapter { symptom ->
            deleteSymptom(symptom)
        }
        binding.recyclerViewSymptoms.apply {
            layoutManager = LinearLayoutManager(this@SymptomsActivity)
            adapter = symptomsAdapter
        }
    }

    private fun setupButtons() {
        binding.btnAddSymptom.setOnClickListener {
            addSymptom()
        }

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun setupDatePicker() {
        updateDateDisplay()
    }

    private fun observeData() {
        viewModel.allSymptoms.observe(this) { symptoms ->
            // 현재 선택된 날짜의 증상만 필터링
            val todaysSymptoms = symptoms.filter { symptom ->
                val symptomCalendar = Calendar.getInstance().apply { time = symptom.date }
                val selectedCalendar = Calendar.getInstance().apply { time = selectedDate }

                symptomCalendar.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                        symptomCalendar.get(Calendar.DAY_OF_YEAR) == selectedCalendar.get(Calendar.DAY_OF_YEAR)
            }
            symptomsAdapter.submitList(todaysSymptoms)
        }
    }

    private fun addSymptom() {
        val selectedSymptomType = getSelectedSymptomType()
        val severity = binding.seekBarSeverity.progress + 1
        val notes = binding.etNotes.text.toString().trim()

        if (selectedSymptomType == null) {
            Toast.makeText(this, "증상을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val symptom = Symptom(
            date = selectedDate,
            symptomType = selectedSymptomType,
            severity = severity,
            notes = notes
        )

        viewModel.insertSymptom(symptom)

        // 입력 필드 초기화
        clearInputFields()

        Toast.makeText(this, "증상이 기록되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun getSelectedSymptomType(): SymptomType? {
        return when (binding.radioGroupSymptoms.checkedRadioButtonId) {
            R.id.radioCramps -> SymptomType.CRAMPS
            R.id.radioHeadache -> SymptomType.HEADACHE
            R.id.radioMoodSwings -> SymptomType.MOOD_SWINGS
            R.id.radioBloating -> SymptomType.BLOATING
            R.id.radioAcne -> SymptomType.ACNE
            R.id.radioFatigue -> SymptomType.FATIGUE
            R.id.radioBreastTenderness -> SymptomType.BREAST_TENDERNESS
            R.id.radioBackPain -> SymptomType.BACK_PAIN
            else -> null
        }
    }

    private fun clearInputFields() {
        binding.radioGroupSymptoms.clearCheck()
        binding.seekBarSeverity.progress = 2 // 기본값 3
        binding.etNotes.text.clear()
    }

    private fun deleteSymptom(symptom: Symptom) {
        lifecycleScope.launch {
            database.symptomDao().deleteSymptom(symptom)
            Toast.makeText(this@SymptomsActivity, "증상이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedDate }

        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                updateDateDisplay()
                loadTodaysSymptoms()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        binding.btnSelectDate.text = dateFormat.format(selectedDate)
        binding.tvSeverityValue.text = "보통 (${binding.seekBarSeverity.progress + 1}/5)"

        // SeekBar 리스너 설정
        binding.seekBarSeverity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val severityText = when (progress + 1) {
                    1 -> "매우 약함"
                    2 -> "약함"
                    3 -> "보통"
                    4 -> "심함"
                    5 -> "매우 심함"
                    else -> "보통"
                }
                binding.tvSeverityValue.text = "$severityText (${progress + 1}/5)"
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun loadTodaysSymptoms() {
        // Observer가 자동으로 업데이트하므로 별도 작업 불필요
    }
}