package com.example.termproject.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.termproject.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AiAnalysisActivity : AppCompatActivity() {

    private var title: String = ""
    private var category: String = ""
    private var selectedOption: String = ""
    private var choiceOptions: String = ""
    private var reason: String = ""
    private var expectedResult: String = ""
    private var emotionScore: Int = 0
    private var createdTime: Long = 0L

    private lateinit var tvDecisionSummary: TextView
    private lateinit var tvDecisionType: TextView
    private lateinit var tvDecisionStyle: TextView
    private lateinit var tvRiskScore: TextView
    private lateinit var tvReasonScore: TextView
    private lateinit var tvStateSummary: TextView
    private lateinit var tvWeatherRelation: TextView
    private lateinit var tvRegretPrediction: TextView
    private lateinit var tvAdvice: TextView
    private lateinit var btnShowAdvice: Button
    private lateinit var btnShare: Button

    private var adviceText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity2_main)

        initViews()
        receiveIntentData()
        showAnalysisResult()
        setupButtons()
    }

    private fun initViews() {
        tvDecisionSummary = findViewById(R.id.tvDecisionSummary)
        tvDecisionType = findViewById(R.id.tvDecisionType)
        tvDecisionStyle = findViewById(R.id.tvDecisionStyle)
        tvRiskScore = findViewById(R.id.tvRiskScore)
        tvReasonScore = findViewById(R.id.tvReasonScore)
        tvStateSummary = findViewById(R.id.tvStateSummary)
        tvWeatherRelation = findViewById(R.id.tvWeatherRelation)
        tvRegretPrediction = findViewById(R.id.tvRegretPrediction)
        tvAdvice = findViewById(R.id.tvAdvice)
        btnShowAdvice = findViewById(R.id.btnShowAdvice)
        btnShare = findViewById(R.id.btnShare)
    }

    private fun receiveIntentData() {
        title = intent.getStringExtra("title") ?: "제목 없음"
        category = intent.getStringExtra("category") ?: "일상"
        choiceOptions = intent.getStringExtra("choiceOptions") ?: "고민한 선택지 없음"
        selectedOption = intent.getStringExtra("selectedOption") ?: "선택 내용 없음"
        reason = intent.getStringExtra("reason") ?: "선택 이유 없음"
        expectedResult = intent.getStringExtra("expectedResult") ?: "기대 결과 없음"
        emotionScore = intent.getIntExtra("emotionScore", 0)
        createdTime = intent.getLongExtra("createdTime", System.currentTimeMillis())
    }

    private fun showAnalysisResult() {
        val decisionType = normalizeCategory(category)
        val emotionPercent = calculateEmotionPercent(emotionScore)
        val logicPercent = 100 - emotionPercent
        val riskScore = calculateRiskScore(decisionType, emotionPercent)
        val reasonScore = calculateReasonScore(reason, choiceOptions)
        val regretLevel = calculateRegretPrediction(emotionPercent, riskScore, reasonScore)
        val decisionTimeText = formatTime(createdTime)

        tvDecisionSummary.text = """
            제목  |  $title
            카테고리  |  $decisionType
            고민한 선택지  |  $choiceOptions
            최종 선택  |  $selectedOption
            선택 이유  |  $reason
            감정 점수  |  ${emotionPercent}점
            결정 시간  |  $decisionTimeText
        """.trimIndent()

                tvDecisionType.text = "📌 결정 유형\n$decisionType"

                tvDecisionStyle.text = """
            💭 결정 성향
            감정 기반 $emotionPercent%  ·  논리 기반 $logicPercent%
        """.trimIndent()

                tvRiskScore.text = """
            ⚠️ 리스크 감수 성향
            ${riskScore}점  ·  ${getRiskMessage(riskScore)}
        """.trimIndent()

                tvReasonScore.text = """
            📝 선택 근거 점수
            ${reasonScore}점  ·  ${getReasonMessage(reasonScore)}
        """.trimIndent()

                tvRegretPrediction.text = """
            🔮 후회 가능성
            $regretLevel
        """.trimIndent()

                tvStateSummary.text = createStateSummary(decisionType, emotionPercent, riskScore)

                tvWeatherRelation.text = """
            현재는 날씨 API 연결 전이에요.
            이후 날씨 API를 연결하면 결정 당시 날씨와 감정 점수를 함께 비교할 수 있어요.
        """.trimIndent()

        adviceText = createAdvice(decisionType, emotionPercent, riskScore, reasonScore)
        tvAdvice.text = "오늘의 조언은 버튼을 누르면 표시됩니다."
    }

    private fun setupButtons() {
        btnShowAdvice.setOnClickListener {
            tvAdvice.text = adviceText
        }

        btnShare.setOnClickListener {
            shareAnalysisResult()
        }
    }

    private fun normalizeCategory(category: String): String {
        return when {
            category.contains("진로") -> "진로"
            category.contains("연애") -> "연애"
            category.contains("관계") || category.contains("인간관계") -> "관계"
            category.contains("소비") -> "소비"
            category.contains("학업") || category.contains("공부") -> "학업"
            category.contains("일") || category.contains("직장") || category.contains("회사") -> "일"
            else -> "일상"
        }
    }

    private fun calculateEmotionPercent(score: Int): Int {
        return when {
            score in 0..7 -> ((score * 100) / 7).coerceIn(0, 100)
            else -> score.coerceIn(0, 100)
        }
    }

    private fun calculateRiskScore(category: String, emotionPercent: Int): Int {
        val categoryBaseScore = when (category) {
            "연애" -> 68
            "관계" -> 62
            "소비" -> 70
            "진로" -> 58
            "학업" -> 45
            "일" -> 55
            else -> 48
        }

        return ((categoryBaseScore + emotionPercent) / 2).coerceIn(0, 100)
    }

    private fun calculateReasonScore(reason: String, expectedResult: String): Int {
        var score = 35

        if (reason.length >= 10) score += 15
        if (reason.length >= 25) score += 20
        if (reason.length >= 50) score += 10
        if (expectedResult.isNotBlank() && expectedResult != "기대 결과 없음") score += 20

        return score.coerceIn(0, 100)
    }

    private fun calculateRegretPrediction(
        emotionPercent: Int,
        riskScore: Int,
        reasonScore: Int
    ): String {
        return when {
            emotionPercent >= 75 && riskScore >= 65 && reasonScore < 60 ->
                "높음 - 감정 영향이 크고 선택 근거가 부족해 보입니다."

            emotionPercent >= 60 || riskScore >= 60 ->
                "보통 - 감정과 리스크가 어느 정도 포함된 결정입니다."

            else ->
                "낮음 - 비교적 안정적이고 근거가 있는 결정으로 보입니다."
        }
    }

    private fun getRiskMessage(score: Int): String {
        return when {
            score >= 70 -> "도전적인 성향이 강한 결정입니다."
            score >= 50 -> "보통 수준의 리스크를 감수한 결정입니다."
            else -> "안정성을 중요하게 생각한 결정입니다."
        }
    }

    private fun getReasonMessage(score: Int): String {
        return when {
            score >= 75 -> "선택 이유와 기대 결과가 비교적 구체적입니다."
            score >= 55 -> "선택 이유는 있으나 조금 더 구체화할 수 있습니다."
            else -> "감정에 비해 선택 근거가 부족할 수 있습니다."
        }
    }

    private fun createStateSummary(
        decisionType: String,
        emotionPercent: Int,
        riskScore: Int
    ): String {
        return when {
            emotionPercent >= 75 && decisionType == "관계" ->
                "당신은 인간관계에서 감정의 영향을 크게 받은 상태로 보입니다. 관계를 회복하거나 정리하고 싶은 마음이 결정에 반영되었을 가능성이 있습니다."

            emotionPercent >= 75 && decisionType == "연애" ->
                "당신은 연애 관련 상황에서 감정적으로 크게 흔들린 상태로 보입니다. 순간적인 서운함이나 기대감이 선택에 영향을 주었을 수 있습니다."

            riskScore >= 70 ->
                "당신은 안정성보다는 변화나 도전을 선택하는 경향이 있었습니다. 결과가 불확실하더라도 시도해보려는 마음이 강했던 상태로 보입니다."

            riskScore <= 45 ->
                "당신은 안정성을 중요시하는 경향이 있었습니다. 큰 변화보다는 손해를 줄이고 상황을 유지하려는 마음이 반영된 결정으로 보입니다."

            else ->
                "당신은 감정과 현실적인 판단을 함께 고려한 상태로 보입니다. 다만 시간이 지난 뒤 실제 결과를 타임캡슐에 기록하면 더 정확한 패턴 분석이 가능합니다."
        }
    }

    private fun createAdvice(
        decisionType: String,
        emotionPercent: Int,
        riskScore: Int,
        reasonScore: Int
    ): String {
        return when {
            emotionPercent >= 75 ->
                "지금의 결정이 틀렸다고 단정하기보다, 감정이 조금 가라앉은 뒤 같은 선택을 다시 봐도 괜찮은지 확인해보세요. 특히 $decisionType 관련 결정은 당시 감정 상태가 만족도에 큰 영향을 줄 수 있습니다."

            reasonScore < 55 ->
                "선택 이유가 아직 충분히 구체적이지 않아 보여요. 다음에는 '왜 이 선택을 했는지', '무엇을 피하고 싶었는지', '어떤 결과를 기대했는지'를 함께 적어보면 후회 패턴을 더 정확히 분석할 수 있습니다."

            riskScore >= 70 ->
                "이번 결정은 도전적인 성향이 강합니다. 좋은 선택이 될 수도 있지만, 나중에 결과를 회고할 때 실제 만족도와 예상 결과가 얼마나 일치했는지 꼭 기록해보세요."

            else ->
                "이번 결정은 비교적 균형 잡힌 선택으로 보입니다. 시간이 지난 뒤 타임캡슐에서 만족도와 후회 여부를 기록하면, 나의 결정 스타일을 더 정확히 알 수 있습니다."
        }
    }

    private fun formatTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return formatter.format(Date(timeMillis))
    }

    private fun shareAnalysisResult() {
        val shareText = """
            [AI 결정 분석 결과]
            
            ${tvDecisionSummary.text}
            
            ${tvDecisionType.text}
            
            ${tvDecisionStyle.text}
            
            ${tvRiskScore.text}
            
            ${tvReasonScore.text}
            
            ${tvStateSummary.text}
            
            ${tvRegretPrediction.text}
            
            ${tvAdvice.text}
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "분석 결과 공유하기"))
    }
}