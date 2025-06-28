package com.ai.papia.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.papia.data.Symptom
import com.ai.papia.data.SymptomType
import com.ai.papia.databinding.ItemSymptomBinding
import java.text.SimpleDateFormat
import java.util.*

class SymptomsAdapter(
    private val onDeleteClick: (Symptom) -> Unit
) : ListAdapter<Symptom, SymptomsAdapter.SymptomViewHolder>(SymptomDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREAN)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomViewHolder {
        val binding = ItemSymptomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SymptomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SymptomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SymptomViewHolder(
        private val binding: ItemSymptomBinding
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
                tvSymptomName.text = symptomName
                tvSeverity.text = "$severityText (${symptom.severity}/5)"
                tvTime.text = timeFormat.format(symptom.date)
                tvNotes.text = if (symptom.notes.isNotEmpty()) symptom.notes else "메모 없음"

                // 심각도에 따른 색상 변경
                val severityColor = when (symptom.severity) {
                    1, 2 -> android.graphics.Color.parseColor("#4CAF50") // 초록
                    3 -> android.graphics.Color.parseColor("#FF9800") // 주황
                    4, 5 -> android.graphics.Color.parseColor("#F44336") // 빨강
                    else -> android.graphics.Color.parseColor("#757575") // 회색
                }

                tvSeverity.setTextColor(severityColor)

                btnDelete.setOnClickListener {
                    onDeleteClick(symptom)
                }
            }
        }
    }

    class SymptomDiffCallback : DiffUtil.ItemCallback<Symptom>() {
        override fun areItemsTheSame(oldItem: Symptom, newItem: Symptom): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Symptom, newItem: Symptom): Boolean {
            return oldItem == newItem
        }
    }
}