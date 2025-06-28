package com.ai.papia.decorator

import android.graphics.Color
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan // 또는 다른 Span

class SelectedDayDecorator(private var day: CalendarDay) : DayViewDecorator {

    // 선택된 날짜의 배경이나 텍스트 스타일을 변경할 Paint 또는 Drawable 정의
    // 예: CircleDrawable
    private val color = Color.BLUE // 선택된 날짜에 표시할 색상

    override fun shouldDecorate(currentDay: CalendarDay): Boolean {
        return currentDay == day
    }

    override fun decorate(view: DayViewFacade) {
        // 배경색 변경 예시
        // view.setBackgroundDrawable(AppCompatResources.getDrawable(view.context, R.drawable.selected_day_background))
        // 또는 특정 스타일의 Span 추가
        view.addSpan(DotSpan(5f, color)) // 파란색 점을 찍는 예시
        // 텍스트 색상 변경 예시
        // view.setTextColor(Color.WHITE)
    }

    // 선택된 날짜가 변경될 때 데코레이터를 업데이트할 수 있는 메서드
    fun setDay(newDay: CalendarDay) {
        this.day = newDay
    }
}