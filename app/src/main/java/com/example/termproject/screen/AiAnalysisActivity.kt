package com.example.termproject.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.termproject.R

class AiAnalysisActivity : AppCompatActivity() {

    private var title: String = ""
    private var category: String = ""
    private var selectedOption: String = ""
    private var reason: String = ""
    private var expectedResult: String = ""
    private var emotionScore: Int = 0
    private var createdTime: Long = 0L

    private lateinit var tvTitle: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvSelectedOption: TextView
    private lateinit var tvReason: TextView
    private lateinit var tvEmotionScore: TextView
    private lateinit var tvDecisionType: TextView
    private lateinit var tvEmotionLogic: TextView
    private lateinit var tvRiskScore: TextView
    private lateinit var tvRegretPossibility: TextView
    private lateinit var tvAdvice: TextView
    private lateinit var btnShare: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity2_main)

        initViews()
        receiveIntentData()
        showInputData()
        showDummyAnalysis()

        btnShare.setOnClickListener {
            shareAnalysisResult()
        }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvCategory = findViewById(R.id.tvCategory)
        tvSelectedOption = findViewById(R.id.tvSelectedOption)
        tvReason = findViewById(R.id.tvReason)
        tvEmotionScore = findViewById(R.id.tvEmotionScore)
        tvDecisionType = findViewById(R.id.tvDecisionType)
        tvEmotionLogic = findViewById(R.id.tvEmotionLogic)
        tvRiskScore = findViewById(R.id.tvRiskScore)
        tvRegretPossibility = findViewById(R.id.tvRegretPossibility)
        tvAdvice = findViewById(R.id.tvAdvice)
        btnShare = findViewById(R.id.btnShare)
    }

    private fun receiveIntentData() {
        title = intent.getStringExtra("title") ?: "제목 없음"
        category = intent.getStringExtra("category") ?: "일상"
        selectedOption = intent.getStringExtra("selectedOption") ?: "선택 내용 없음"
        reason = intent.getStringExtra("reason") ?: "선택 이유 없음"
        expectedResult = intent.getStringExtra("expectedResult") ?: "기대 결과 없음"
        emotionScore = intent.getIntExtra("emotionScore", 0)
        createdTime = intent.getLongExtra("createdTime", System.currentTimeMillis())
    }

    private fun showInputData() {
        tvTitle.text = "제목: $title"
        tvCategory.text = "카테고리: $category"
        tvSelectedOption.text = "선택한 것: $selectedOption"
        tvReason.text = "선택 이유: $reason"
        tvEmotionScore.text = "감정 점수: $emotionScore"
    }

    private fun showDummyAnalysis() {
        val emotionRatio = emotionScore.coerceIn(0, 100)
        val logicRatio = 100 - emotionRatio
        val riskScore = calculateRiskScore(emotionScore)

        tvDecisionType.text = "결정 유형: $category 관련 결정"
        tvEmotionLogic.text = "감정 기반 $emotionRatio% / 논리 기반 $logicRatio%"
        tvRiskScore.text = "리스크 감수 성향: ${riskScore}점"
        tvRegretPossibility.text = "후회 가능성: ${getRegretLevel(emotionScore)}"
        tvAdvice.text = "AI 조언: 현재 감정이 선택에 어느 정도 반영된 상태로 보입니다. 시간이 지난 뒤 타임캡슐에서 실제 결과를 다시 기록해보세요."
    }

    private fun calculateRiskScore(score: Int): Int {
        return when {
            score >= 80 -> 75
            score >= 50 -> 55
            else -> 35
        }
    }

    private fun getRegretLevel(score: Int): String {
        return when {
            score >= 80 -> "높음"
            score >= 50 -> "보통"
            else -> "낮음"
        }
    }

    private fun shareAnalysisResult() {
        val shareText = """
            [AI 결정 분석 결과]
            
            제목: $title
            카테고리: $category
            선택한 것: $selectedOption
            선택 이유: $reason
            감정 점수: $emotionScore
            
            ${tvDecisionType.text}
            ${tvEmotionLogic.text}
            ${tvRiskScore.text}
            ${tvRegretPossibility.text}
            ${tvAdvice.text}
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "분석 결과 공유하기"))
    }
}