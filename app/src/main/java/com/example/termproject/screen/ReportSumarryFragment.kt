package com.example.termproject.screen

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.termproject.R

class ReportSummaryFragment : Fragment(R.layout.fragment_report_summary) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ReportRepository.loadRecords(
            onSuccess = { records ->
                val monthlyRecords = ReportAnalyzer.getThisMonthRecords(records)
                val report = ReportAnalyzer.analyze(monthlyRecords)

                view.findViewById<TextView>(R.id.tvMonthTitle).text =
                    "${report.monthTitle} 결정 리포트"

                view.findViewById<TextView>(R.id.tvDecisionStyle).text = report.styleTitle
                view.findViewById<TextView>(R.id.tvHeroSummary).text = report.heroSummary

                view.findViewById<TextView>(R.id.tvHealthScore).text =
                    "${report.healthScore}점"

                view.findViewById<TextView>(R.id.tvCountBox).text =
                    "결정 수\n\n${report.totalCount}개"

                view.findViewById<TextView>(R.id.tvSatisfactionBox).text =
                    "평균 만족도\n\n${String.format("%.1f", report.avgSatisfaction)}점"

                view.findViewById<TextView>(R.id.tvRegretBox).text =
                    "평균 후회율\n\n${report.avgRegretRate}%"

                view.findViewById<TextView>(R.id.tvMostCategoryBox).text =
                    "최다 카테고리\n\n${report.mostCategory}"
            },
            onFailure = {
                Toast.makeText(requireContext(), "리포트를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}