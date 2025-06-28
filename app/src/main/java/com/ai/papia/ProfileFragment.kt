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
import com.ai.papia.data.UserProfile
import com.ai.papia.databinding.FragmentProfileBinding
import com.ai.papia.room.PeriodTrackerDatabase
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: PeriodTrackerDatabase
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    private var selectedBirthDate: Date? = null
    private var selectedBirthControlStartDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        database = PeriodTrackerDatabase.getDatabase(requireContext())
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        setupClickListeners()
        observeData()
    }

    private fun setupClickListeners() {
        binding.btnSelectBirthDate.setOnClickListener {
            showDatePicker { date ->
                selectedBirthDate = date
                binding.tvBirthDate.text = dateFormat.format(date)
            }
        }

        binding.btnSelectBirthControlStart.setOnClickListener {
            showDatePicker { date ->
                selectedBirthControlStartDate = date
                binding.tvBirthControlStartDate.text = dateFormat.format(date)
            }
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun observeData() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                populateFields(it)
            }
        }
    }

    private fun populateFields(profile: UserProfile) {
        binding.etName.setText(profile.name)

        profile.birthDate?.let {
            selectedBirthDate = it
            binding.tvBirthDate.text = dateFormat.format(it)
        }

        profile.height?.let {
            binding.etHeight.setText(it.toString())
        }

        profile.weight?.let {
            binding.etWeight.setText(it.toString())
        }

        binding.etBirthControlType.setText(profile.birthControlType)

        profile.birthControlStartDate?.let {
            selectedBirthControlStartDate = it
            binding.tvBirthControlStartDate.text = dateFormat.format(it)
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(calendar.time)
        }, year, month, day).show()
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val heightText = binding.etHeight.text.toString().trim()
        val weightText = binding.etWeight.text.toString().trim()
        val birthControlType = binding.etBirthControlType.text.toString().trim()

        val height = if (heightText.isNotEmpty()) heightText.toFloatOrNull() else null
        val weight = if (weightText.isNotEmpty()) weightText.toFloatOrNull() else null

        if (height != null && (height < 100 || height > 250)) {
            binding.etHeight.error = "올바른 키를 입력하세요 (100-250cm)"
            return
        }

        if (weight != null && (weight < 20 || weight > 200)) {
            binding.etWeight.error = "올바른 몸무게를 입력하세요 (20-200kg)"
            return
        }

        val profile = UserProfile(
            name = name,
            birthDate = selectedBirthDate,
            height = height,
            weight = weight,
            birthControlType = birthControlType,
            birthControlStartDate = selectedBirthControlStartDate
        )

        lifecycleScope.launch {
            try {
                val existingProfile = database.userProfileDao().getProfileBlocking()

                if (existingProfile == null) {
                    database.userProfileDao().insertProfile(profile)
                } else {
                    database.userProfileDao().updateProfile(profile)
                }
                Toast.makeText(requireContext(), "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
