package com.ai.papia.decorator

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.util.*

/**
 * MaterialCalendarView에서 생리 기간 미리보기를 위한 데코레이터입니다.
 * 사용자가 기간을 확정하기 전에 선택된 범위를 시각적으로 표시합니다.
 */
class PreviewPeriodDecorator(private val dates: Collection<CalendarDay>, context: Context) : DayViewDecorator {

    // 미리보기 기간을 위한 배경색 (예: #80FFC107은 50% 투명도의 Amber 색상)
    // 이 색상은 임시이며, 원하는 색상으로 변경 가능합니다.
    private val highlightDrawable = ColorDrawable(Color.parseColor("#80FFC107"))

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.setBackgroundDrawable(highlightDrawable)
        // 필요하다면, 미리보기 기간의 날짜 텍스트 색상도 변경할 수 있습니다.
        // view.setTextColor(Color.DKGRAY)
    }
}