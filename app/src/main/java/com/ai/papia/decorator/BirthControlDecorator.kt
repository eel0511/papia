package com.ai.papia.decorator

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.style.LineBackgroundSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

/**
 * 피임약 복용일을 달력에 표시하는 데코레이터입니다.
 * 해당 날짜 아래에 선을 그립니다.
 */
class BirthControlDecorator(private val dates: Collection<CalendarDay>) : DayViewDecorator {

    private val paint = Paint().apply {
        color = 0xFF800080.toInt() // 보라색 선 (ARGB)
        strokeWidth = 6f // 선의 두께
    }

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // 날짜 텍스트 아래에 선을 그리는 커스텀 Span 추가
        view.addSpan(object : LineBackgroundSpan {
            override fun drawBackground(
                canvas: Canvas,
                paint: Paint,
                left: Int,
                right: Int,
                top: Int,
                baseline: Int,
                bottom: Int,
                text: CharSequence,
                start: Int,
                end: Int,
                lineNumber: Int
            ) {
                // 텍스트 아래에 선을 그립니다.
                // bottom은 텍스트의 하단 경계입니다.
                // baseline은 텍스트의 기준선입니다.
                // 텍스트 자체의 하단에 가깝게 그리기 위해 bottom을 사용합니다.
                val lineY = bottom + 5 // 텍스트 하단에서 약간 아래에 그립니다.
                canvas.drawLine(left.toFloat() + 10, lineY.toFloat(), right.toFloat() - 10, lineY.toFloat(), this@BirthControlDecorator.paint)
            }
        })
    }
}