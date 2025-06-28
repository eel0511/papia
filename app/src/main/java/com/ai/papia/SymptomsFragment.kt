package com.ai.papia

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.papia.adapters.SymptomsAdapter
import com.ai.papia.data.Symptom
import com.ai.papia.data.SymptomType
import com.ai.papia.databinding.FragmentSymptomsBinding
import com.ai.papia.room.PeriodTrackerDatabase
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SymptomsFragment : Fragment() {

    private var _binding: FragmentSymptomsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: PeriodTrackerDatabase
    private lateinit var symptomsAdapter: SymptomsAdapter
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    private var selectedDate = Calendar.getInstance().time

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSymptomsBinding.inflate(inflater, container, false)
        database = PeriodTrackerDatabase.getDatabase(requireContext())
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        setupRecyclerView()
        setupButtons()
        updateDateDisplay()
        observeData()
    }
    private fun setupRecyclerView() {
        symptomsAdapter = SymptomsAdapter { symptom ->
            deleteSymptom(symptom)
        }
        binding.recyclerViewSymptoms.apply {
            layoutManager = LinearLayoutManager(requireContext())
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

    private fun observeData() {
        viewModel.allSymptoms.observe(viewLifecycleOwner) { symptoms ->
            val todaysSymptoms = symptoms.filter { it.sameDay(selectedDate) }
            symptomsAdapter.submitList(todaysSymptoms)
        }
    }

    private fun addSymptom() {
        val selectedSymptomType = getSelectedSymptomType()
        val severity = binding.seekBarSeverity.progress + 1
        val notes = binding.etNotes.text.toString().trim()

        if (selectedSymptomType == null) {
            Toast.makeText(requireContext(), "증상을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val symptom = Symptom(
            date = selectedDate,
            symptomType = selectedSymptomType,
            severity = severity,
            notes = notes
        )

        viewModel.insertSymptom(symptom)
        clearInputFields()
        Toast.makeText(requireContext(), "증상이 기록되었습니다.", Toast.LENGTH_SHORT).show()
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
        binding.seekBarSeverity.progress = 2
        binding.etNotes.text.clear()
    }

    private fun deleteSymptom(symptom: Symptom) {
        lifecycleScope.launch {
            database.symptomDao().deleteSymptom(symptom)
            Toast.makeText(requireContext(), "증상이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedDate }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                updateDateDisplay()
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

    private fun Symptom.sameDay(date: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = this@sameDay.date }
        val cal2 = Calendar.getInstance().apply { time = date }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
