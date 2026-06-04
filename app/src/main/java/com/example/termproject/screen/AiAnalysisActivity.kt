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

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.termproject.BuildConfig
import com.example.termproject.network.OpenAiChatRequest
import com.example.termproject.network.OpenAiMessage
import com.example.termproject.network.RetrofitClient
import kotlinx.coroutines.launch

class AiAnalysisActivity : AppCompatActivity() {

    private val useGptApi = false

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
    private lateinit var btnNewDecision: Button
    private lateinit var btnTimeCapsule: Button

    private var adviceText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity2_main)

        initViews()
        receiveIntentData()
        showAnalysisResult()
        if (useGptApi) {
            requestGptFullAnalysis()
        }
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
        btnNewDecision = findViewById(R.id.btnNewDecision)
        btnTimeCapsule = findViewById(R.id.btnTimeCapsule)
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

    private fun requestGptFullAnalysis() {
        val apiKey = BuildConfig.OPENAI_API_KEY

        if (apiKey.isBlank()) {
            Toast.makeText(
                this,
                "API 키가 없어 기본 분석을 표시합니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        tvDecisionType.text = "📌 결정 유형\nAI가 분석 중이에요..."
        tvDecisionStyle.text = "💭 결정 성향\nAI가 감정과 논리 비율을 분석 중이에요..."
        tvRiskScore.text = "⚠️ 리스크 감수 성향\nAI가 리스크 점수를 분석 중이에요..."
        tvReasonScore.text = "📝 선택 근거 점수\nAI가 선택 이유를 분석 중이에요..."
        tvRegretPrediction.text = "🔮 후회 가능성\nAI가 후회 가능성을 분석 중이에요..."
        tvStateSummary.text = "AI가 오늘의 상태를 요약하고 있어요."
        tvWeatherRelation.text = "AI가 날씨와 감정의 관계를 분석하고 있어요."

        lifecycleScope.launch {
            try {
                val request = OpenAiChatRequest(
                    model = "gpt-4o-mini",
                    max_tokens = 650,
                    temperature = 0.75,
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content =  """
                                너는 사용자의 결정 기록을 자연스럽게 해석해주는 한국어 AI야.
                                분석은 딱딱한 보고서처럼 쓰지 말고, 앱 사용자가 읽기 편한 말투로 작성해.
                                "때문입니다", "근거는", "결과적으로" 같은 표현을 반복하지 마.
                                사용자를 판단하거나 단정하지 말고, "~로 보여요", "~일 수 있어요", "~에 가까워 보여요"처럼 부드럽게 말해.
                                각 항목은 짧지만 자연스럽게 이어지는 문장으로 작성해.
                            """.trimIndent()
                        ),
                        OpenAiMessage(
                            role = "user",
                            content = createFullAnalysisPrompt()
                        )
                    )
                )

                val response = RetrofitClient.openAiApiService.createChatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )

                val result = response.choices
                    .firstOrNull()
                    ?.message
                    ?.content
                    ?.trim()

                if (result.isNullOrBlank()) {
                    Toast.makeText(
                        this@AiAnalysisActivity,
                        "AI 분석 응답이 비어 있어 기본 분석을 표시합니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    showAnalysisResult()
                } else {
                    applyGptFullAnalysis(result)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@AiAnalysisActivity,
                    "AI 분석 생성 실패: 기본 분석을 표시합니다.",
                    Toast.LENGTH_SHORT
                ).show()
                showAnalysisResult()
            }
        }
    }

    private fun applyGptFullAnalysis(result: String) {
        val decisionTypeText = extractSection(result, "DECISION_TYPE", "분석 결과를 불러오지 못했습니다.")
        val decisionStyleText = extractSection(result, "DECISION_STYLE", "감정/논리 분석 결과를 불러오지 못했습니다.")
        val riskScoreText = extractSection(result, "RISK_SCORE", "리스크 분석 결과를 불러오지 못했습니다.")
        val reasonScoreText = extractSection(result, "REASON_SCORE", "선택 근거 분석 결과를 불러오지 못했습니다.")
        val regretPredictionText = extractSection(result, "REGRET_PREDICTION", "후회 가능성 분석 결과를 불러오지 못했습니다.")
        val stateSummaryText = extractSection(result, "STATE_SUMMARY", "상태 요약을 불러오지 못했습니다.")
        val weatherRelationText = extractSection(result, "WEATHER_RELATION", "날씨와 감정 분석 결과를 불러오지 못했습니다.")

        tvDecisionType.text = "📌 결정 유형\n\n$decisionTypeText"
        tvDecisionStyle.text = "💭 결정 성향\n\n$decisionStyleText"
        tvRiskScore.text = "⚠️ 리스크 감수 성향\n\n$riskScoreText"
        tvReasonScore.text = "📝 선택 근거 점수\n\n$reasonScoreText"
        tvRegretPrediction.text = "🔮 후회 가능성\n\n$regretPredictionText"

        tvStateSummary.text = stateSummaryText
        tvWeatherRelation.text = weatherRelationText
    }

    private fun extractSection(
        text: String,
        key: String,
        fallback: String
    ): String {
        val regex = Regex(
            "\\[$key\\]\\s*([\\s\\S]*?)(?=\\n\\[[A-Z_]+\\]|$)"
        )

        return regex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: fallback
    }

    private fun createFullAnalysisPrompt(): String {
        val decisionType = normalizeCategory(category)
        val emotionPercent = calculateEmotionPercent(emotionScore)
        val decisionTimeText = formatTime(createdTime)

        return """
            다음은 사용자가 방금 작성한 결정 기록입니다.
    
            [입력 데이터]
            제목: $title
            카테고리: $decisionType
            고민한 선택지: $choiceOptions
            최종 선택: $selectedOption
            선택 이유: $reason
            감정 점수: $emotionPercent / 100
            결정 시간: $decisionTimeText
    
            아래 섹션 태그는 파싱을 위해 반드시 그대로 유지해주세요.
            섹션 태그 이름은 바꾸지 말고, 각 태그 아래 내용만 작성해주세요.
            마크다운 기호 *, #, - 는 사용하지 마세요.
    
            전체 작성 스타일:
            1. 한국어로 작성
            2. 너무 보고서처럼 딱딱하게 쓰지 않기
            3. "때문입니다"를 반복하지 않기
            4. "결과는 ~, 근거는 ~"처럼 기계적인 구조로 쓰지 않기
            5. 사용자가 적은 선택 이유와 최종 선택을 자연스럽게 반영하기
            6. 심리 진단처럼 단정하지 말고 "~로 보여요", "~일 수 있어요", "~에 가까워요"처럼 표현하기
            7. 각 섹션은 1~2문장 정도로 짧고 읽기 쉽게 작성하기
    
            [DECISION_TYPE]
            진로/연애/관계/소비/학업/일상/일 중 가장 가까운 결정 유형을 자연스럽게 설명해주세요.
            예: "이 결정은 학업과 관련된 선택에 가까워 보여요. 수업을 계속 들을지 말지 고민한 점에서, 당장의 부담과 미래의 후회를 함께 고려한 결정으로 보입니다."
    
            [DECISION_STYLE]
            감정 기반과 논리 기반 비율을 합쳐서 100%가 되도록 제시해주세요.
            단순히 숫자만 말하지 말고, 사용자의 선택 이유가 감정 쪽에 가까운지 현실적인 판단 쪽에 가까운지 자연스럽게 설명해주세요.
            예: "감정 기반 60%, 논리 기반 40% 정도로 볼 수 있어요. '나중에 더 후회할 것 같아서'라는 표현에는 불안감도 있지만, 미래의 결과를 생각한 판단도 함께 들어 있습니다."
    
            [RISK_SCORE]
            리스크 감수 성향을 0~100점으로 제시하고, 안정형/균형형/도전형 중 하나로 자연스럽게 설명해주세요.
            예: "리스크 감수 성향은 58점 정도로, 균형형에 가까워 보여요. 완전히 안전한 선택만 한 것은 아니지만, 무작정 도전하기보다는 후회 가능성을 줄이려는 쪽에 무게가 있습니다."
    
            [REASON_SCORE]
            선택 근거 점수를 0~100점으로 제시하고, 사용자의 선택 이유가 얼마나 구체적인지 부드럽게 평가해주세요.
            예: "선택 근거 점수는 70점 정도로 볼 수 있어요. 이유는 분명하지만, 앞으로 어떻게 버틸지까지 적혀 있다면 더 선명한 결정 기록이 될 수 있습니다."
    
            [REGRET_PREDICTION]
            후회 가능성을 낮음/보통/높음 중 하나로 말하고, 너무 단정하지 않게 설명해주세요.
            예: "후회 가능성은 보통 정도로 보여요. 선택 자체보다, 이후에 구체적인 계획 없이 버티기만 하면 만족도가 낮아질 수 있습니다."
    
            [STATE_SUMMARY]
            오늘의 상태를 2문장 정도로 자연스럽게 요약해주세요.
            안정성을 중요시한 상태인지, 도전적인 상태인지, 감정에 조금 흔들린 상태인지 사용자의 기록에 맞게 설명해주세요.
    
            [WEATHER_RELATION]
            아직 실제 날씨 데이터가 Activity2에 직접 연결되어 있지 않으므로 날씨를 단정하지 마세요.
            대신 "날씨 데이터가 연결되면 감정 점수와 함께 비교할 수 있다"는 방향으로 자연스럽게 작성해주세요.
        """.trimIndent()
    }

    private fun setupButtons() {
        btnShowAdvice.setOnClickListener {
            if (useGptApi) {
                requestGptAdvice()
            } else {
                tvAdvice.text = adviceText
                Toast.makeText(
                    this,
                    "개발 모드: 기본 조언을 표시합니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnNewDecision.setOnClickListener {
            val intent = Intent(this, Activity1::class.java)
            startActivity(intent)
            finish()
        }

        btnTimeCapsule.setOnClickListener {
            Toast.makeText(
                this,
                "타임캡슐 기능은 아직 준비 중입니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestGptAdvice() {
        val apiKey = BuildConfig.OPENAI_API_KEY

        if (apiKey.isBlank()) {
            tvAdvice.text = adviceText
            Toast.makeText(
                this,
                "API 키가 없어 기본 조언을 표시합니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnShowAdvice.isEnabled = false
        btnShowAdvice.text = "조언 생성 중..."
        tvAdvice.text = "AI가 오늘의 조언을 생성하고 있어요."

        lifecycleScope.launch {
            try {
                val prompt = createAdvicePrompt()

                val request = OpenAiChatRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content = """
                                너는 사용자의 결정 기록을 바탕으로 짧고 현실적인 조언을 해주는 한국어 AI야.
                                말투는 딱딱한 상담문이 아니라, 앱에서 보여주는 부드러운 회고 문장처럼 작성해.
                                "해야 합니다", "필요합니다", "중요합니다" 같은 표현을 반복하지 말고,
                                "~해보면 좋아요", "~를 확인해볼 수 있어요", "~에 가까워 보여요"처럼 자연스럽게 말해.
                                사용자를 판단하거나 비난하지 말고, 사용자가 적은 선택 이유를 꼭 반영해.
                            """.trimIndent()
                        ),
                        OpenAiMessage(
                            role = "user",
                            content = prompt
                        )
                    )
                )

                val response = RetrofitClient.openAiApiService.createChatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )

                val gptAdvice = response.choices
                    .firstOrNull()
                    ?.message
                    ?.content
                    ?.trim()

                if (gptAdvice.isNullOrBlank()) {
                    tvAdvice.text = adviceText
                    Toast.makeText(
                        this@AiAnalysisActivity,
                        "응답이 비어 있어 기본 조언을 표시합니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    tvAdvice.text = gptAdvice
                    adviceText = gptAdvice
                }

            } catch (e: Exception) {
                e.printStackTrace()

                tvAdvice.text = """
                    기본 조언:
                    $adviceText
                    
                    오류 내용:
                    ${e.message}
                """.trimIndent()

                Toast.makeText(
                    this@AiAnalysisActivity,
                    "AI 조언 생성 실패: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnShowAdvice.isEnabled = true
                btnShowAdvice.text = "오늘의 조언 다시 보기"
            }
        }
    }

    private fun createAdvicePrompt(): String {
        val decisionType = normalizeCategory(category)
        val emotionPercent = calculateEmotionPercent(emotionScore)
        val riskScore = calculateRiskScore(decisionType, emotionPercent)
        val reasonScore = calculateReasonScore(reason, choiceOptions)

        return """
        사용자의 결정 기록을 분석해서 개인화된 조언을 작성해줘.

        [결정 기록]
        제목: $title
        카테고리: $decisionType
        고민한 선택지: $choiceOptions
        최종 선택: $selectedOption
        선택 이유: $reason
        감정 점수: $emotionPercent / 100
        리스크 감수 성향 점수: $riskScore / 100
        선택 근거 점수: $reasonScore / 100

        [작성 조건]
        1. 한국어로 작성
        2. 너무 추상적인 위로 금지
        3. 반드시 사용자가 실제로 적은 선택 이유를 자연스럽게 반영
        4. 위의 핵심 분석 화면과 비슷한 부드러운 말투 사용
        5. "때문입니다", "필요합니다", "중요합니다" 같은 딱딱한 표현 반복 금지
        6. "~해보면 좋아요", "~를 확인해볼 수 있어요", "~일 수 있어요"처럼 자연스럽게 작성
        7. 아래 형식을 그대로 사용
        8. 결정 해석과 주의할 점은 각각 1~3 문장정도, 그리고 지금해볼 행동과 타임캡슐 질문은 1~2문장 정도로 짧게 작성
        9. 의학적 진단, 심리 상담처럼 단정하는 표현 금지

        [출력 형식]
        🔍 결정 해석:
        사용자가 왜 이 선택을 했는지 구체적으로 해석해줘.

        ⚖️ 주의할 점:
        이 결정에서 나중에 후회가 생길 수 있는 포인트를 알려줘.

        ✅ 지금 해볼 행동:
        오늘 또는 내일 바로 할 수 있는 현실적인 행동 1가지를 제안해줘.

        ⏳ 타임캡슐 질문:
        나중에 이 결정을 돌아볼 때 답하면 좋은 질문 1개를 만들어줘.
    """.trimIndent()

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

}