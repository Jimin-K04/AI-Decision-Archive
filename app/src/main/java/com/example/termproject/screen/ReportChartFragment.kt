package com.example.termproject.screen

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.termproject.R

class ReportChartFragment : Fragment(R.layout.fragment_report_chart) {

    private val barColors = listOf(
        "#E65B3B",
        "#F6B26B",
        "#6FA8A8",
        "#A88CCB",
        "#D98C9F",
        "#B7A66A"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val barContainer = view.findViewById<LinearLayout>(R.id.barChartContainer)
        val pieChart = view.findViewById<ReportPieChartView>(R.id.pieCategoryChart)
        val pieLegend = view.findViewById<LinearLayout>(R.id.pieLegendContainer)

        ReportRepository.loadRecords(
            onSuccess = { records ->
                val monthlyRecords = ReportAnalyzer.getThisMonthRecords(records)
                val report = ReportAnalyzer.analyze(monthlyRecords)

                pieChart.setData(report.categoryCounts)
                buildPieLegend(pieLegend, report.categoryCounts, pieChart.chartColors)

                barContainer.removeAllViews()

                addTitle(barContainer, "카테고리별 기록 수")
                addBarChart(barContainer, report.categoryCounts)

                addTitle(barContainer, "주차별 결정 수")
                addBarChart(barContainer, report.weeklyCounts)

                addTitle(barContainer, "낮 vs 밤 만족도")
                addBarChart(
                    barContainer,
                    listOf(
                        "낮" to report.daySatisfaction,
                        "밤" to report.nightSatisfaction
                    )
                )

                addTitle(barContainer, "감정 점수별 만족도")
                addBarChart(barContainer, report.emotionSatisfaction)
            },
            onFailure = {
                Toast.makeText(requireContext(), "그래프를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun buildPieLegend(
        container: LinearLayout,
        data: List<Pair<String, Int>>,
        colors: List<Int>
    ) {
        container.removeAllViews()

        val total = data.sumOf { it.second }

        if (total == 0) {
            val empty = TextView(requireContext()).apply {
                text = "카테고리 데이터 없음"
                textSize = 14f
                setTextColor(Color.parseColor("#8D7F73"))
            }
            container.addView(empty)
            return
        }

        data.forEachIndexed { index, item ->
            val percent = (item.second * 100f / total).toInt()

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(5), 0, dp(5))
            }

            val dot = View(requireContext()).apply {
                background = roundedDrawable(colors[index % colors.size], dp(99))
                layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply {
                    rightMargin = dp(10)
                    topMargin = dp(4)
                }
            }

            val text = TextView(requireContext()).apply {
                this.text = "${item.first}  $percent% · ${item.second}건"
                textSize = 14f
                setTextColor(Color.parseColor("#5B4B42"))
            }

            row.addView(dot)
            row.addView(text)
            container.addView(row)
        }
    }

    private fun addTitle(container: LinearLayout, title: String) {
        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 18f
            setTextColor(Color.parseColor("#24150F"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(26), 0, dp(12))
        }

        container.addView(titleView)
    }

    private fun addBarChart(container: LinearLayout, data: List<Pair<String, Int>>) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            background = resources.getDrawable(R.drawable.report_bg_card, null)
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }

        val maxValue = data.maxOfOrNull { it.second } ?: 1

        data.forEachIndexed { index, item ->
            val label = TextView(requireContext()).apply {
                text = "${item.first}  ${item.second}"
                textSize = 14f
                setTextColor(Color.parseColor("#5B4B42"))
                setPadding(0, dp(10), 0, dp(6))
            }

            val track = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                background = resources.getDrawable(R.drawable.report_bg_bar_track, null)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(18)
                )
            }

            val barWidth = if (maxValue == 0) {
                0
            } else {
                ((item.second.toFloat() / maxValue) * dp(260)).toInt()
            }

            val bar = View(requireContext()).apply {
                background = roundedDrawable(Color.parseColor(barColors[index % barColors.size]), dp(99))
                layoutParams = LinearLayout.LayoutParams(
                    barWidth.coerceAtLeast(dp(16)),
                    dp(18)
                )
            }

            track.addView(bar)
            card.addView(label)
            card.addView(track)
        }

        container.addView(card)
    }

    private fun roundedDrawable(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}