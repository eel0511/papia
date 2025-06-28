package com.ai.papia

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ai.papia.data.UserProfile
import com.ai.papia.databinding.ActivityProfileBinding
import com.ai.papia.room.PeriodTrackerDatabase
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var database: PeriodTrackerDatabase
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    private var selectedBirthDate: Date? = null
    private var selectedLastPeriodDate: Date? = null
    private var selectedBirthControlStartDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = PeriodTrackerDatabase.getDatabase(this)

        setupToolbar()
        setupClickListeners()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "개인 정보 설정"
    }

    private fun setupClickListeners() {
        binding.btnSelectBirthDate.setOnClickListener {
            showDatePicker { date ->
                selectedBirthDate = date
                binding.tvBirthDate.text = dateFormat.format(date)
            }
        }
//
//        binding.btnSelectLastPeriod.setOnClickListener {
//            showDatePicker { date ->
//                selectedLastPeriodDate = date
//                binding.tvLastPeriodDate.text = dateFormat.format(date)
//            }
//        }

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
        viewModel.userProfile.observe(this) { profile ->
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
//
//        binding.etCycleLength.setText(profile.averageCycleLength.toString())
//        binding.etPeriodLength.setText(profile.averagePeriodLength.toString())
//
//        profile.lastPeriodStartDate?.let {
//            selectedLastPeriodDate = it
//            binding.tvLastPeriodDate.text = dateFormat.format(it)
//        }

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

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(calendar.time)
        }, year, month, day).show()
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val heightText = binding.etHeight.text.toString().trim()
        val weightText = binding.etWeight.text.toString().trim()
//        val cycleLengthText = binding.etCycleLength.text.toString().trim()
//        val periodLengthText = binding.etPeriodLength.text.toString().trim()
        val birthControlType = binding.etBirthControlType.text.toString().trim()

        // 유효성 검사
//        if (cycleLengthText.isEmpty()) {
//            binding.etCycleLength.error = "주기 길이를 입력하세요"
//            return
//        }
//
//        if (periodLengthText.isEmpty()) {
//            binding.etPeriodLength.error = "생리 기간을 입력하세요"
//            return
//        }

        val height = if (heightText.isNotEmpty()) heightText.toFloatOrNull() else null
        val weight = if (weightText.isNotEmpty()) weightText.toFloatOrNull() else null
//        val cycleLength = cycleLengthText.toIntOrNull() ?: 28
//        val periodLength = periodLengthText.toIntOrNull() ?: 5

        if (height != null && (height < 100 || height > 250)) {
            binding.etHeight.error = "올바른 키를 입력하세요 (100-250cm)"
            return
        }

        if (weight != null && (weight < 20 || weight > 200)) {
            binding.etWeight.error = "올바른 몸무게를 입력하세요 (20-200kg)"
            return
        }

//        if (cycleLength < 20 || cycleLength > 45) {
//            binding.etCycleLength.error = "주기는 20-45일 사이여야 합니다"
//            return
//        }

//        if (periodLength < 1 || periodLength > 10) {
//            binding.etPeriodLength.error = "생리 기간은 1-10일 사이여야 합니다"
//            return
//        }

        val profile = UserProfile(
            name = name,
            birthDate = selectedBirthDate,
            height = height,
            weight = weight,
//            averageCycleLength = cycleLength,
//            averagePeriodLength = periodLength,
            lastPeriodStartDate = selectedLastPeriodDate,
            birthControlType = birthControlType,
            birthControlStartDate = selectedBirthControlStartDate
        )

        lifecycleScope.launch {
            try {
                val existingProfile = database.userProfileDao().getProfileBlocking()

                if (existingProfile == null) {
                    // 프로필이 없는 경우, 새로 삽입합니다.
                    database.userProfileDao().insertProfile(profile)
                    Log.d("ProfileActivity", "새로운 프로필이 삽입되었습니다.")
                } else {
                    // 프로필이 이미 존재하는 경우, 업데이트합니다.
                    database.userProfileDao().updateProfile(profile)
                    Log.d("ProfileActivity", "기존 프로필이 업데이트되었습니다.")
                }
                Toast.makeText(this@ProfileActivity, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}