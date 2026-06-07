package com.example.termproject.screen

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.termproject.BuildConfig
import com.example.termproject.R
import com.example.termproject.network.OpenAiChatRequest
import com.example.termproject.network.OpenAiMessage
import com.example.termproject.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AiAnalysisActivity : AppCompatActivity() {

    private val useGptApi = true

    private var title: String = ""
    private var category: String = ""
    private var selectedOption: String = ""
    private var choiceOptions: String = ""
    private var reason: String = ""
    private var expectedResult: String = ""
    private var emotionScore: Int = 0
    private var createdTime: Long = 0L

    private var weatherText: String = "알 수 없음"
    private var temperature: Double = 0.0
    private var humidity: Int = 0
    private var precipitation: Double = 0.0
    private var rain: Double = 0.0
    private var cloudCover: Int = 0
    private var pm10: Double = 0.0
    private var pm25: Double = 0.0
    private var uvIndex: Double = 0.0
    private var discomfortIndex: Double = 0.0
    private var discomfortText: String = "알 수 없음"

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
        showLoadingFallback()

        if (useGptApi) {
            requestGptFullAnalysis()
        } else {
            showBasicFallbackAnalysis()
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

        weatherText = intent.getStringExtra("weatherText") ?: "알 수 없음"
        temperature = intent.getDoubleExtra("temperature", 0.0)
        humidity = intent.getIntExtra("humidity", 0)
        precipitation = intent.getDoubleExtra("precipitation", 0.0)
        rain = intent.getDoubleExtra("rain", 0.0)
        cloudCover = intent.getIntExtra("cloudCover", 0)
        pm10 = intent.getDoubleExtra("pm10", 0.0)
        pm25 = intent.getDoubleExtra("pm25", 0.0)
        uvIndex = intent.getDoubleExtra("uvIndex", 0.0)
        discomfortIndex = intent.getDoubleExtra("discomfortIndex", 0.0)
        discomfortText = intent.getStringExtra("discomfortText") ?: "알 수 없음"
    }

    private fun showLoadingFallback() {
        val decisionType = normalizeCategory(category)
        val decisionTimeText = formatTime(createdTime)
        val emotionPercent = calculateEmotionPercent(emotionScore)

        tvDecisionSummary.text = """
제목 | $title
카테고리 | $decisionType
고민한 선택지 | $choiceOptions
최종 선택 | $selectedOption
선택 이유 | $reason
감정 점수 | ${emotionPercent}점/100점
결정 시간 | $decisionTimeText
        """.trimIndent()

        tvDecisionType.text = "📌 결정 유형\nAI가 분석 중이에요..."
        tvDecisionStyle.text = "💭 결정 성향\nAI가 감정과 논리 비율을 계산 중이에요..."
        tvRiskScore.text = "⚠️ 리스크 감수 성향\nAI가 리스크 점수를 계산 중이에요..."
        tvReasonScore.text = "📝 선택 근거 점수\nAI가 선택 근거 점수를 계산 중이에요..."
        tvRegretPrediction.text = "🔮 후회 가능성\nAI가 후회 가능성을 분석 중이에요..."
        tvStateSummary.text = "AI가 오늘의 상태를 요약하고 있어요."
        tvWeatherRelation.text = "AI가 날씨와 감정의 관계를 분석하고 있어요."
        tvAdvice.text = "오늘의 조언은 버튼을 누르면 표시됩니다."
    }

    private fun requestGptFullAnalysis() {
        val apiKey = BuildConfig.OPENAI_API_KEY

        if (apiKey.isBlank()) {
            Toast.makeText(this, "API 키가 없어 기본 분석을 표시합니다.", Toast.LENGTH_SHORT).show()
            showBasicFallbackAnalysis()
            return
        }

        lifecycleScope.launch {
            try {
                val request = OpenAiChatRequest(
                    model = "gpt-4o-mini",
                    max_tokens = 600,
                    temperature = 0.65,
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content = """
                        너는 사용자의 결정 기록을 분석하는 한국어 AI야.
                        반드시 사용자가 준 기록, 감정 점수, 선택지, 선택 이유, 날씨 데이터를 함께 고려해서 판단해.
                        카테고리 하나만으로 점수나 비율을 정하지 마.
                        점수와 비율은 네가 직접 계산하되, 판단 기준을 짧게 설명해.
                        사용자를 단정하거나 평가하지 말고 "~로 보여요", "~했을 수 있어요"처럼 부드럽게 말해.
                        마크다운 기호 *, #, - 는 사용하지 마.
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
                    Toast.makeText(this@AiAnalysisActivity, "AI 응답이 비어 있어 기본 분석을 표시합니다.", Toast.LENGTH_SHORT).show()
                    showBasicFallbackAnalysis()
                } else {
                    applyGptFullAnalysis(result)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AiAnalysisActivity, "AI 분석 실패: 기본 분석을 표시합니다.", Toast.LENGTH_SHORT).show()
                showBasicFallbackAnalysis()
            }
        }
    }

    private fun applyGptFullAnalysis(result: String) {
        val decisionTypeText = extractSection(result, "DECISION_TYPE", "일상\n입력된 내용을 기준으로 일상적인 결정에 가까워 보여요.")
        val decisionStyleText = extractSection(result, "DECISION_STYLE", "감정 기반 50% · 논리 기반 50%\n감정과 현실적인 판단이 함께 들어간 결정으로 보여요.")
        val riskScoreText = extractSection(result, "RISK_SCORE", "50점/100점 · 균형형\n결과가 완전히 확실하지는 않지만 무리한 선택으로 보이진 않아요.")
        val reasonScoreText = extractSection(result, "REASON_SCORE", "50점/100점\n선택 이유는 있지만 조금 더 구체화하면 더 좋은 기록이 될 수 있어요.")
        val regretPredictionText = extractSection(result, "REGRET_PREDICTION", "보통\n선택 이후의 행동에 따라 만족도가 달라질 수 있어요.")
        val stateSummaryText = extractSection(result, "STATE_SUMMARY", "오늘은 감정과 현실적인 이유를 함께 고려한 상태로 보여요.")
        val weatherRelationText = extractSection(result, "WEATHER_RELATION", "날씨 정보와 감정 점수를 함께 보면 결정 당시의 컨디션을 더 입체적으로 볼 수 있어요.")

        setAnalysisFromGpt(tvDecisionType, "📌 결정 유형", decisionTypeText)
        setAnalysisFromGpt(tvDecisionStyle, "💭 결정 성향", decisionStyleText)
        setAnalysisFromGpt(tvRiskScore, "⚠️ 리스크 감수 성향", riskScoreText)
        setAnalysisFromGpt(tvReasonScore, "📝 선택 근거 점수", reasonScoreText)
        setAnalysisFromGpt(tvRegretPrediction, "🔮 후회 가능성", regretPredictionText)

        tvStateSummary.text = stateSummaryText
        tvWeatherRelation.text = weatherRelationText

        adviceText = createBasicAdvice()
    }

    private fun setAnalysisFromGpt(
        textView: TextView,
        title: String,
        gptText: String
    ) {
        val lines = gptText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val mainText = lines.firstOrNull() ?: "분석 결과 없음"
        val description = lines.drop(1).joinToString("\n")

        val fullText = if (description.isBlank()) {
            "$title\n$mainText"
        } else {
            "$title\n$mainText\n$description"
        }

        val spannable = SpannableString(fullText)

        val titleStart = fullText.indexOf(title)
        val titleEnd = titleStart + title.length
        if (titleStart >= 0) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                titleStart,
                titleEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val mainStart = fullText.indexOf(mainText)
        val mainEnd = mainStart + mainText.length
        if (mainStart >= 0) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                mainStart,
                mainEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                RelativeSizeSpan(1.03f),
                mainStart,
                mainEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.text = spannable
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
        사용자가 직접 입력한 감정 점수: $emotionPercent / 100
        결정 시간: $decisionTimeText
        
        [날씨 데이터]
        날씨: $weatherText
        기온: ${temperature}℃
        습도: ${humidity}%
        강수량: $precipitation
        비: $rain
        구름량: $cloudCover%
        체감 상태: $discomfortText
        PM10: $pm10
        PM2.5: $pm25
        자외선 지수: $uvIndex
        
        [중요한 판단 기준]
        1. 결정 성향
        감정 기반 비율과 논리 기반 비율을 합쳐서 반드시 100%로 계산해주세요.
        감정 기반은 사용자의 감정 점수, 선택 이유의 감정 표현, 충동성, 불안, 관계 회복 욕구, 후회 회피 같은 정서적 동기를 반영해주세요.
        논리 기반은 선택지 비교, 현실적 이유, 장단점 고려, 미래 결과 고려, 구체적 근거, 선택 이후 계획을 반영해주세요.
        카테고리만으로 판단하지 마세요.
        
        2. 리스크 감수 성향
        0~100점으로 판단해주세요.
        점수가 높을수록 결과가 불확실해도 선택을 밀고 간 결정입니다.
        판단 기준은 결과의 불확실성, 되돌리기 어려움, 선택 이후 부담, 감정에 밀려 결정한 정도, 대안 검토 여부, 선택 실패 시 손실 가능성입니다.
        카테고리만으로 점수를 정하지 마세요.
        0~39점은 안정형, 40~69점은 균형형, 70~100점은 도전형으로 표시해주세요.
        점수 뒤에 "/100점"은 붙이지 마세요.
        
        3. 선택 근거 점수
        0~100점으로 판단해주세요.
        점수가 높을수록 사용자가 나중에 덜 후회할 가능성이 있는 근거 있는 선택입니다.
        판단 기준은 선택 이유의 구체성, 고민한 선택지 비교, 예상 결과 고려, 감정과 현실의 균형, 선택 이후 행동 계획, 후회 가능성을 줄이는 근거입니다.
        단순히 글자 수만으로 판단하지 마세요.
        
        4. 후회 가능성
        낮음/보통/높음으로 판단해주세요.
        감정 비율이 높고 선택 근거가 약하면 높게 판단해주세요.
        선택 근거가 구체적이고 리스크가 관리 가능하면 낮게 판단해주세요.
        리스크가 높아도 사용자가 이유와 계획을 충분히 세웠다면 무조건 높음으로 보지 마세요.
        
        5. 결정 유형은 사용자가 선택한 카테고리를 그대로 사용하고, GPT가 임의로 바꾸지 마세요.
        
        [출력 규칙]
        아래 섹션 태그는 파싱을 위해 반드시 그대로 유지해주세요.
        섹션 태그 이름은 바꾸지 마세요.
        각 태그 아래 내용만 작성해주세요.
        마크다운 기호 *, #, - 는 사용하지 마세요.
        각 핵심 분석 섹션은 첫 줄에 핵심값, 그 아래에 이유 1~2문장만 작성해주세요.
        
        [DECISION_TYPE]
        첫 줄에는 사용자가 선택한 카테고리 "$decisionType" 를 그대로 작성해주세요.
        카테고리를 새로 분류하거나 바꾸지 마세요.
        둘째 줄에는 이 카테고리 안에서 어떤 고민으로 보이는지 한 문장만 짧게 작성해주세요.
        
        [DECISION_STYLE]
        첫 줄에는 반드시 "감정 기반 n% · 논리 기반 n%" 형식으로 작성해주세요.
        둘째 줄부터는 왜 그 비율인지 1~2문장만 짧게 작성해주세요.
        
        [RISK_SCORE]
        첫 줄에는 반드시 "n점 · 안정형/균형형/도전형" 형식으로 작성해주세요.
        둘째 줄부터는 왜 그 점수인지 1문장만 작성해주세요.
        
        [REASON_SCORE]
        첫 줄에는 반드시 "n점" 형식으로 작성해주세요.
        둘째 줄부터는 왜 그 점수인지 1~2문장만 짧게 작성해주세요.
        
        [REGRET_PREDICTION]
        첫 줄에는 반드시 "낮음/보통/높음" 중 하나만 작성해주세요.
        둘째 줄부터는 왜 그렇게 판단했는지 1~2문장만 작성해주세요.
        
        [STATE_SUMMARY]
        점수 이름을 다시 나열하지 말고, 사용자의 실제 상황과 선택 이유를 반영해서 오늘의 상태를 2~3문장으로 자연스럽게 요약해주세요.
        날씨가 컨디션에 영향을 줬을 수 있다면 부드럽게 연결해주세요.
        
        [WEATHER_RELATION]
        오늘 날씨를 기온, 습도, 미세먼지, 자외선, 체감 상태를 포함해서 2~3문장으로 설명해주세요.
        그 뒤에 날씨가 감정이나 판단 분위기에 어떤 영향을 줬을 수 있는지 1~2문장으로 연결해주세요.
        단, 날씨 때문에 결정했다고 단정하지 말고 "~했을 수 있어요"처럼 작성해주세요.
        
        """.trimIndent()
    }

    private fun setupButtons() {
        btnShowAdvice.setOnClickListener {
            if (useGptApi) {
                requestGptAdvice()
            } else {
                tvAdvice.text = createBasicAdvice()
                Toast.makeText(this, "기본 조언을 표시합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        btnNewDecision.setOnClickListener {
            val intent = Intent(this, Activity1::class.java)
            startActivity(intent)
            finish()
        }

        btnTimeCapsule.setOnClickListener {
            Toast.makeText(this, "타임캡슐 기능은 아직 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestGptAdvice() {
        val apiKey = BuildConfig.OPENAI_API_KEY

        if (apiKey.isBlank()) {
            tvAdvice.text = createBasicAdvice()
            Toast.makeText(this, "API 키가 없어 기본 조언을 표시합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        btnShowAdvice.isEnabled = false
        btnShowAdvice.text = "조언 생성 중..."
        tvAdvice.text = "AI가 오늘의 조언을 생성하고 있어요."

        lifecycleScope.launch {
            try {
                val request = OpenAiChatRequest(
                    model = "gpt-4o-mini",
                    max_tokens = 500,
                    temperature = 0.75,
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content = """
                너는 사용자의 결정 기록을 바탕으로 따뜻하고 현실적인 조언을 해주는 한국어 AI야.
                사용자를 판단하거나 혼내지 말고, 사용자가 적은 선택 이유를 꼭 반영해.
                너무 추상적인 위로만 하지 말고 오늘 또는 내일 할 수 있는 행동을 구체적으로 제안해.
                            """.trimIndent()
                        ),
                        OpenAiMessage(
                            role = "user",
                            content = createAdvicePrompt()
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

                tvAdvice.text = if (gptAdvice.isNullOrBlank()) {
                    createBasicAdvice()
                } else {
                    gptAdvice
                }

            } catch (e: Exception) {
                e.printStackTrace()
                tvAdvice.text = createBasicAdvice()
                Toast.makeText(this@AiAnalysisActivity, "AI 조언 생성 실패: 기본 조언을 표시합니다.", Toast.LENGTH_LONG).show()
            } finally {
                btnShowAdvice.isEnabled = true
                btnShowAdvice.text = "오늘의 조언 다시 보기"
            }
        }
    }

    private fun createAdvicePrompt(): String {
        val decisionType = normalizeCategory(category)
        val emotionPercent = calculateEmotionPercent(emotionScore)

        return """
사용자의 결정 기록을 바탕으로 개인화된 조언을 작성해주세요.

[결정 기록]
제목: $title
카테고리: $decisionType
고민한 선택지: $choiceOptions
최종 선택: $selectedOption
선택 이유: $reason
사용자가 입력한 감정 점수: $emotionPercent / 100

[날씨 데이터]
날씨: $weatherText
기온: ${temperature}℃
습도: ${humidity}%
체감 상태: $discomfortText
PM10: $pm10
PM2.5: $pm25
자외선 지수: $uvIndex

[작성 조건]
1. 한국어로 작성
2. 따뜻하고 다정한 말투 사용
3. 사용자의 선택을 평가하거나 혼내지 않기
4. 사용자가 적은 선택 이유를 반드시 반영하기
5. 너무 추상적인 위로 금지
6. 오늘 바로 할 수 있는 행동을 구체적으로 제안하기
7. "~해보면 좋아요", "~도 괜찮아요", "~를 한번 확인해보세요" 같은 부드러운 표현 사용
8. 각 항목은 2~3문장 정도로 작성
9. 의학적 진단, 심리 상담처럼 단정하는 표현 금지
10. 사용자가 이 결정을 나중에 덜 후회하도록 도와주는 방향으로 작성

[출력 형식]
🔍 결정 해석:
사용자가 왜 이 선택을 했는지 구체적으로 해석해주세요.

⚖️ 주의할 점:
이 결정에서 나중에 후회가 생길 수 있는 포인트를 알려주세요.

✅ 지금 해볼 행동:
오늘 또는 내일 바로 할 수 있는 현실적인 행동 1가지를 제안해주세요.

⏳ 타임캡슐 질문:
나중에 이 결정을 돌아볼 때 답하면 좋은 질문 1개를 만들어주세요.
        """.trimIndent()
    }

    private fun showBasicFallbackAnalysis() {
        val decisionType = normalizeCategory(category)
        val emotionPercent = calculateEmotionPercent(emotionScore)
        val logicPercent = 100 - emotionPercent

        setAnalysisFromGpt(
            tvDecisionType,
            "📌 결정 유형",
            "$decisionType\n입력한 카테고리와 선택 내용을 기준으로 $decisionType 결정에 가까워 보여요."
        )

        setAnalysisFromGpt(
            tvDecisionStyle,
            "💭 결정 성향",
            "감정 기반 $emotionPercent% · 논리 기반 $logicPercent%\n현재 기본 분석에서는 사용자가 입력한 감정 점수를 기준으로 임시 계산했어요."
        )

        setAnalysisFromGpt(
            tvRiskScore,
            "⚠️ 리스크 감수 성향",
            "50점 · 균형형\nAI 분석을 사용할 수 없어 기본값으로 표시했어요."
        )

        setAnalysisFromGpt(
            tvReasonScore,
            "📝 선택 근거 점수",
            "50점\nAI 분석을 사용할 수 없어 기본값으로 표시했어요."
        )

        setAnalysisFromGpt(
            tvRegretPrediction,
            "🔮 후회 가능성",
            "보통\nAI 분석을 사용할 수 없어 기본값으로 표시했어요."
        )

        tvStateSummary.text = """
오늘은 감정과 현실적인 이유를 함께 고려해 선택하려 한 상태로 보여요.
선택 이유를 조금 더 구체적으로 남기면 나중에 이 결정을 돌아볼 때 더 도움이 될 수 있어요.
        """.trimIndent()

        tvWeatherRelation.text = """
결정 당시 날씨는 $weatherText 이에요.
기온은 ${temperature}℃, 습도는 ${humidity}%였고, 체감 상태는 $discomfortText 에 가까웠어요.

PM10은 $pm10, PM2.5는 $pm25, 자외선 지수는 $uvIndex 였어요.
이런 날씨와 컨디션은 감정 점수와 함께 보면 결정 당시의 분위기를 이해하는 데 도움이 될 수 있어요.
        """.trimIndent()

        adviceText = createBasicAdvice()
    }

    private fun createBasicAdvice(): String {
        return """
🔍 결정 해석:
지금의 선택은 단순히 맞고 틀린 결정이라기보다, 당시의 마음과 상황이 함께 반영된 선택으로 보여요.

⚖️ 주의할 점:
나중에 후회가 생긴다면 선택 자체보다, 선택 후에 아무 행동도 하지 않았을 때 더 크게 느껴질 수 있어요.

✅ 지금 해볼 행동:
오늘 안에 이 선택을 잘 이어가기 위해 할 수 있는 작은 행동 하나를 정해보면 좋아요.

⏳ 타임캡슐 질문:
나중에 돌아봤을 때, 이 선택은 내 마음을 더 편하게 해줬나요?
        """.trimIndent()
    }

    private fun extractSection(
        text: String,
        key: String,
        fallback: String
    ): String {
        val regex = Regex("\\[$key\\]\\s*([\\s\\S]*?)(?=\\n\\[[A-Z_]+\\]|$)")
        return regex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: fallback
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

    private fun formatTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return formatter.format(Date(timeMillis))
    }
}