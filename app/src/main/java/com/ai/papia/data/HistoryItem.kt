package com.ai.papia.data


sealed class HistoryItem {
    data class PeriodItem(val period: PeriodRecord) : HistoryItem()
    data class BirthControlItem(val record: BirthControlRecord) : HistoryItem()
    data class SymptomItem(val symptom: Symptom) : HistoryItem()
}