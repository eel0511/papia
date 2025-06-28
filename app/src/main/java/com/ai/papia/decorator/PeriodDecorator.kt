package com.ai.papia.decorator

import android.content.Context
import com.ai.papia.R
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

// Period Decorator Class
class PeriodDecorator(private val dates: Collection<CalendarDay>,private val context: Context) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        if ( context.getDrawable(R.drawable.period_day_background) != null) {
            view.setBackgroundDrawable( context.getDrawable(R.drawable.period_day_background)!!)
        } else {
            // Drawable이 없을 경우 fallback으로 DotSpan을 사용하거나, 다른 방식으로 표시
            // 예시로 점을 찍거나, 전체 배경색을 설정할 수 있습니다.
            // view.addSpan(DotSpan(8f, periodColor)) // 점을 찍는 방식
            // 또는 배경색을 직접 설정 (단색 배경의 경우)
            // view.setBackgroundDrawable(ColorDrawable(periodColor))
            // 이 예시에서는 R.drawable.period_highlight_background를 사용하도록 가이드합니다.
            // 아래에서 period_highlight_background 정의 예시를 제공합니다.
        }
        view.setDaysDisabled(false) // 기간 내 날짜는 비활성화하지 않음
    }
}