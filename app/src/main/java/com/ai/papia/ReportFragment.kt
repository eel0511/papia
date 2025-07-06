package com.ai.papia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ai.papia.databinding.FragmentReportBinding
import com.ai.papia.viewModel.MainViewModel
import com.ai.papia.viewModel.MainViewModelFactory
import com.ai.papia.room.PeriodTrackerDatabase
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate

class ReportFragment : Fragment() {
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: PeriodTrackerDatabase
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        database = PeriodTrackerDatabase.getDatabase(requireContext())
        setupBarChart()
        observePeriodData()
        return binding.root
    }

    private fun setupBarChart() {
        val barChart: BarChart = binding.barChart
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setPinchZoom(false)
        barChart.setScaleEnabled(false)
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.granularity = 1f
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.legend.isEnabled = false
    }

    private fun observePeriodData() {
        viewModel.allPeriods.observe(viewLifecycleOwner) { periods ->
            // 예시: 최근 7회 생리 기간을 막대그래프로 표시
            val entries = periods.take(7).mapIndexed { idx, period ->
                val length = period.endDate?.let { ((it.time - period.startDate.time) / (1000 * 60 * 60 * 24)).toFloat() + 1 } ?: 0f
                BarEntry(idx.toFloat(), length)
            }
            val dataSet = BarDataSet(entries, "생리 기간(일)")
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            val barData = BarData(dataSet)
            barData.barWidth = 0.5f
            binding.barChart.data = barData
            binding.barChart.invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 