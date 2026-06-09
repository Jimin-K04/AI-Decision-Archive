package com.example.termproject.screen

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.termproject.R
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan

class ReportStyleFragment : Fragment(R.layout.fragment_report_style) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ReportRepository.loadRecords(
            onSuccess = { records ->
                val monthlyRecords = ReportAnalyzer.getThisMonthRecords(records)
                val report = ReportAnalyzer.analyze(monthlyRecords)

                view.findViewById<TextView>(R.id.tvStyleBadge).text = getStyleEmoji(report.styleTitle)
                view.findViewById<TextView>(R.id.tvStyleTitle).text = report.styleTitle
                view.findViewById<TextView>(R.id.tvStyleSubtitle).text = getStyleSubTitle(report.styleTitle)

                view.findViewById<TextView>(R.id.tvStyleSummary).text =
                    buildCardText("🧭 스타일 분석", report.styleSummary)

                view.findViewById<TextView>(R.id.tvRecommend).text =
                    buildCardText("💡 맞춤 추천", report.recommendText)

                view.findViewById<TextView>(R.id.tvTopSatisfied).text =
                    buildCardText("🏆 이번 달 만족한 결정 TOP3", buildTopBody(report.topSatisfied, true))

                view.findViewById<TextView>(R.id.tvTopRegretted).text =
                    buildCardText("🫧 이번 달 후회한 결정 TOP3", buildTopBody(report.topRegretted, false))
            },
            onFailure = {
                Toast.makeText(requireContext(), "스타일 분석을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun buildCardText(title: String, body: String): SpannableString {
        val fullText = "$title\n\n$body"
        val spannable = SpannableString(fullText)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            title.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private fun getStyleEmoji(style: String): String {
        return when (style) {
            "안전형" -> "🧸"
            "도전형" -> "✨"
            "감정형" -> "💌"
            "회피형" -> "🌙"
            else -> "📊"
        }
    }

    private fun getStyleSubTitle(style: String): String {
        return when (style) {
            "안전형" -> "신중하고 안정적인 선택을 선호해요"
            "도전형" -> "새로운 선택을 시도하는 편이에요"
            "감정형" -> "감정의 흐름을 잘 반영하는 타입이에요"
            "회피형" -> "결정 전 고민이 깊어지는 편이에요"
            else -> "기록을 쌓으며 분석 중이에요"
        }
    }

    private fun buildTopBody(
        items: List<ReportRecord>,
        satisfactionMode: Boolean
    ): String {
        if (items.isEmpty()) {
            return "아직 회고 데이터가 부족해요."
        }

        return items.mapIndexed { index, item ->
            val scoreText = if (satisfactionMode) {
                "만족도 ${item.satisfaction ?: 0}/5"
            } else {
                "후회율 ${ReportAnalyzer.regretToScore(item.regret)}%"
            }

            "${index + 1}. ${item.title.ifBlank { item.category }}\n   $scoreText · ${item.category}"
        }.joinToString("\n\n")
    }
}