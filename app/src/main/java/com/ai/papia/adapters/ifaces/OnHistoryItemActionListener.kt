package com.ai.papia.adapters.ifaces

import com.ai.papia.data.BirthControlRecord
import com.ai.papia.data.PeriodRecord
import com.ai.papia.data.Symptom

interface OnHistoryItemActionListener {
    fun onDeletePeriod(record: PeriodRecord)
    fun onDeleteBirthControl(record: BirthControlRecord)
    fun onDeleteSymptom(record: Symptom)
}