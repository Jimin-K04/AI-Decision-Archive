package com.example.termproject.screen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ReportPieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<Pair<String, Int>> = emptyList()

    val chartColors = listOf(
        Color.parseColor("#E65B3B"), // 메인 코랄
        Color.parseColor("#F6B26B"), // 살구 오렌지
        Color.parseColor("#6FA8A8"), // 차분한 민트
        Color.parseColor("#A88CCB"), // 부드러운 라벤더
        Color.parseColor("#D98C9F"), // 로즈핑크
        Color.parseColor("#B7A66A")  // 올리브 베이지
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setData(items: List<Pair<String, Int>>) {
        data = items.filter { it.second > 0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val total = data.sumOf { it.second }
        if (total <= 0) return

        val size = width.coerceAtMost(height).toFloat()
        val padding = 34f
        val left = (width - size) / 2 + padding
        val top = (height - size) / 2 + padding
        val right = left + size - padding * 2
        val bottom = top + size - padding * 2

        var startAngle = -90f

        data.forEachIndexed { index, item ->
            val sweepAngle = item.second / total.toFloat() * 360f
            paint.color = chartColors[index % chartColors.size]
            canvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }

        paint.color = Color.parseColor("#FAF7F1")
        canvas.drawCircle(width / 2f, height / 2f, size * 0.22f, paint)
    }
}