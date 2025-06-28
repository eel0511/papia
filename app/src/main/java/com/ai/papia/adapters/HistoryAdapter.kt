package com.ai.papia.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.papia.R
import com.ai.papia.adapters.ifaces.OnHistoryItemActionListener
import com.ai.papia.data.BirthControlRecord
import com.ai.papia.data.HistoryItem
import com.ai.papia.data.PeriodRecord
import com.ai.papia.data.Symptom
import com.ai.papia.data.SymptomType
import com.ai.papia.databinding.ItemHistoryBirthControlBinding
import com.ai.papia.databinding.ItemHistoryPeriodBinding
import com.ai.papia.databinding.ItemHistorySymptomBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val listener: OnHistoryItemActionListener,
    private val dateFormat: SimpleDateFormat
) : ListAdapter<HistoryItem, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val TYPE_PERIOD = 0
        private const val TYPE_BIRTH_CONTROL = 1
        private const val TYPE_SYMPTOM = 2
    }

    private var currentDataType: String = "periods" // periods, birth_control, symptoms

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryItem.PeriodItem -> TYPE_PERIOD
            is HistoryItem.BirthControlItem -> TYPE_BIRTH_CONTROL
            is HistoryItem.SymptomItem -> TYPE_SYMPTOM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PERIOD -> {
                val binding = ItemHistoryPeriodBinding.inflate(inflater, parent, false)
                PeriodViewHolder(binding)
            }
            TYPE_BIRTH_CONTROL -> {
                val binding = ItemHistoryBirthControlBinding.inflate(inflater, parent, false)
                BirthControlViewHolder(binding)
            }
            TYPE_SYMPTOM -> {
                val binding = ItemHistorySymptomBinding.inflate(inflater, parent, false)
                SymptomViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PeriodViewHolder -> holder.bind((getItem(position) as HistoryItem.PeriodItem).period)
            is BirthControlViewHolder -> holder.bind((getItem(position) as HistoryItem.BirthControlItem).record)
            is SymptomViewHolder -> holder.bind((getItem(position) as HistoryItem.SymptomItem).symptom)
        }
    }

    // 생리 기록 리스트 제출
    fun submitPeriodList(periods: List<PeriodRecord>) {
        currentDataType = "periods"
        val historyItems = periods.map { HistoryItem.PeriodItem(it) }
        submitList(historyItems)
    }

    // 피임약 기록 리스트 제출
    fun submitBirthControlList(records: List<BirthControlRecord>) {
        currentDataType = "birth_control"
        val historyItems = records.map { HistoryItem.BirthControlItem(it) }
        submitList(historyItems)
    }

    // 증상 기록 리스트 제출
    fun submitSymptomsList(symptoms: List<Symptom>) {
        currentDataType = "symptoms"
        val historyItems = symptoms.map { HistoryItem.SymptomItem(it) }
        submitList(historyItems)
    }

    // 생리 기록 ViewHolder
    inner class PeriodViewHolder(
        private val binding: ItemHistoryPeriodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(period: PeriodRecord) {
            binding.apply {
                tvStartDate.text = "시작일: ${dateFormat.format(period.startDate)}"

                if (period.endDate != null) {
                    tvEndDate.text = "종료일: ${dateFormat.format(period.endDate)}"
                    tvEndDate.visibility = View.VISIBLE

                    // 생리 기간 계산
                    val startCal = Calendar.getInstance().apply { time = period.startDate }
                    val endCal = Calendar.getInstance().apply { time = period.endDate }
                    val days = ((endCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
                    tvDuration.text = "기간: ${days}일"
                    tvDuration.visibility = View.VISIBLE
                } else {
                    tvEndDate.visibility = View.GONE
                    tvDuration.text = "진행 중"
                    tvDuration.visibility = View.VISIBLE
                }

                // 유량 표시
                val flowText = when (period.flow) {
                    com.ai.papia.data.PeriodFlow.LIGHT -> "적음"
                    com.ai.papia.data.PeriodFlow.MEDIUM -> "보통"
                    com.ai.papia.data.PeriodFlow.HEAVY -> "많음"
                }
                tvFlow.text = "유량: $flowText"

                // 메모 표시
                if (period.notes.isNotEmpty()) {
                    tvNotes.text = period.notes
                    tvNotes.visibility = View.VISIBLE
                } else {
                    tvNotes.visibility = View.GONE
                }
            }
            binding.btnDeletePeriod.setOnClickListener {
                listener.onDeletePeriod(period)
            }
        }
    }

    // 피임약 기록 ViewHolder
    inner class BirthControlViewHolder(
        private val binding: ItemHistoryBirthControlBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: BirthControlRecord) {
            binding.apply {
                tvDate.text = dateFormat.format(record.date)

                if (record.taken) {
                    tvStatus.text = "복용함"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    ivStatus.setImageResource(R.drawable.ic_check_circle)
                    ivStatus.setColorFilter(itemView.context.getColor(android.R.color.holo_green_dark))
                } else {
                    tvStatus.text = "복용 안함"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    ivStatus.setImageResource(R.drawable.ic_cancel)
                    ivStatus.setColorFilter(itemView.context.getColor(android.R.color.holo_red_dark))
                }

                if (record.timesTaken > 1) {
                    tvTimesTaken.text = "복용 횟수: ${record.timesTaken}회"
                    tvTimesTaken.visibility = View.VISIBLE
                } else {
                    tvTimesTaken.visibility = View.GONE
                }

                if (record.notes.isNotEmpty()) {
                    tvNotes.text = record.notes
                    tvNotes.visibility = View.VISIBLE
                } else {
                    tvNotes.visibility = View.GONE
                }
            }
            binding.btnDeleteBirthControl.setOnClickListener {
                listener.onDeleteBirthControl(record)
            }
        }
    }

    // 증상 기록 ViewHolder
    inner class SymptomViewHolder(
        private val binding: ItemHistorySymptomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(symptom: Symptom) {
            val symptomName = when (symptom.symptomType) {
                SymptomType.CRAMPS -> "생리통"
                SymptomType.HEADACHE -> "두통"
                SymptomType.MOOD_SWINGS -> "기분 변화"
                SymptomType.BLOATING -> "복부팽만"
                SymptomType.ACNE -> "여드름"
                SymptomType.FATIGUE -> "피로감"
                SymptomType.BREAST_TENDERNESS -> "가슴 압통"
                SymptomType.BACK_PAIN -> "허리 통증"
            }

            val severityText = when (symptom.severity) {
                1 -> "매우 약함"
                2 -> "약함"
                3 -> "보통"
                4 -> "심함"
                5 -> "매우 심함"
                else -> "보통"
            }

            binding.apply {
                tvDate.text = dateFormat.format(symptom.date)
                tvSymptomName.text = symptomName
                tvSeverity.text = "$severityText (${symptom.severity}/5)"

                // 심각도에 따른 색상 설정
                val severityColor = when (symptom.severity) {
                    1, 2 -> itemView.context.getColor(android.R.color.holo_green_dark)
                    3 -> itemView.context.getColor(android.R.color.holo_orange_dark)
                    4, 5 -> itemView.context.getColor(android.R.color.holo_red_dark)
                    else -> itemView.context.getColor(android.R.color.darker_gray)
                }
                tvSeverity.setTextColor(severityColor)

                if (symptom.notes.isNotEmpty()) {
                    tvNotes.text = symptom.notes
                    tvNotes.visibility = View.VISIBLE
                } else {
                    tvNotes.visibility = View.GONE
                }
            }
            binding.btnDeleteSymptom.setOnClickListener {
                listener.onDeleteSymptom(symptom)
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return when {
                oldItem is HistoryItem.PeriodItem && newItem is HistoryItem.PeriodItem ->
                    oldItem.period.id == newItem.period.id
                oldItem is HistoryItem.BirthControlItem && newItem is HistoryItem.BirthControlItem ->
                    oldItem.record.id == newItem.record.id
                oldItem is HistoryItem.SymptomItem && newItem is HistoryItem.SymptomItem ->
                    oldItem.symptom.id == newItem.symptom.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return when {
                oldItem is HistoryItem.PeriodItem && newItem is HistoryItem.PeriodItem ->
                    oldItem.period == newItem.period
                oldItem is HistoryItem.BirthControlItem && newItem is HistoryItem.BirthControlItem ->
                    oldItem.record == newItem.record
                oldItem is HistoryItem.SymptomItem && newItem is HistoryItem.SymptomItem ->
                    oldItem.symptom == newItem.symptom
                else -> false
            }
        }
    }
}