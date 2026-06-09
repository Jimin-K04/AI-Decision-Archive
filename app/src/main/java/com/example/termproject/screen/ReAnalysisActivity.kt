package com.example.termproject.screen

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import android.text.Spannable

class ReAnalysisActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var recordId: String = ""

    private lateinit var tvStateSummary: TextView
    private lateinit var tvChipCategory: TextView
    private lateinit var tvChipStyle: TextView
    private lateinit var tvChipReview: TextView
    private lateinit var tvDecisionSummary: TextView
    private lateinit var tvReviewSummary: TextView
    private lateinit var tvPatternSummary: TextView
    private lateinit var tvReAnalysisResult: TextView
    private lateinit var btnRetry: Button
    private lateinit var btnBack: Button

    private var record = ReAnalysisRecord()
    private var review = ReAnalysisReview()
    private var reviewDocId: String? = null
    private var report: ReportResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_re_analysis)

        recordId = intent.getStringExtra("recordId") ?: ""

        initViews()
        setupButtons()

        if (recordId.isBlank()) {
            Toast.makeText(this, "기록 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading()
        loadData()
    }

    private fun initViews() {
        tvStateSummary = findViewById(R.id.tvStateSummary)
        tvChipCategory = findViewById(R.id.tvChipCategory)
        tvChipStyle = findViewById(R.id.tvChipStyle)
        tvChipReview = findViewById(R.id.tvChipReview)
        tvDecisionSummary = findViewById(R.id.tvDecisionSummary)
        tvReviewSummary = findViewById(R.id.tvReviewSummary)
        tvPatternSummary = findViewById(R.id.tvPatternSummary)
        tvReAnalysisResult = findViewById(R.id.tvReAnalysisResult)
        btnRetry = findViewById(R.id.btnRetry)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupButtons() {
        btnRetry.setOnClickListener {
            requestGptReAnalysis()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showLoading() {
        tvStateSummary.text = "AI가 과거의 선택과 현재의 회고를 다시 연결해보고 있어요."
        tvDecisionSummary.text = "결정 기록을 불러오는 중입니다."
        tvReviewSummary.text = "타임캡슐 회고를 불러오는 중입니다."
        tvPatternSummary.text = "월간 통계를 불러오는 중입니다."
        tvReAnalysisResult.text = "AI 재분석을 준비하고 있어요."
        btnRetry.isEnabled = false
        btnRetry.text = "분석 중..."
    }

    private fun loadData() {
        db.collection("records")
            .document(recordId)
            .get()
            .addOnSuccessListener { doc ->
                record = ReAnalysisRecord(
                    title = doc.getString("title") ?: "",
                    category = cleanCategory(doc.getString("category") ?: "기타"),
                    choiceOptions = doc.getString("choiceOptions") ?: "",
                    selectedOption = doc.getString("selectedOption") ?: "",
                    reason = doc.getString("reason") ?: "",
                    expectedResult = doc.getString("expectedResult") ?: "",
                    emotionScore = doc.getLong("emotionScore")?.toInt() ?: 0,
                    weather = doc.getString("weather") ?: "알 수 없음",
                    temperature = doc.getDouble("temperature") ?: 0.0,
                    humidity = doc.getLong("humidity")?.toInt() ?: 0,
                    discomfort = doc.getString("discomfort") ?: "",
                    createdAt = doc.getTimestamp("createdAt")
                )

                loadLatestReview()
            }
            .addOnFailureListener {
                Toast.makeText(this, "결정 기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadLatestReview() {
        db.collection("records")
            .document(recordId)
            .collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull()
                reviewDocId = doc?.id

                review = if (doc != null) {
                    ReAnalysisReview(
                        satisfaction = doc.getLong("satisfaction")?.toInt() ?: 0,
                        result = doc.getString("result") ?: "",
                        regret = doc.getString("regret") ?: "",
                        retryChoice = doc.getString("retryChoice") ?: "",
                        memo = doc.getString("memo") ?: "",
                        previousReAnalysis = doc.getString("reAnalysisText") ?: ""
                    )
                } else {
                    ReAnalysisReview()
                }

                loadReport()
            }
            .addOnFailureListener {
                loadReport()
            }
    }

    private fun loadReport() {
        ReportRepository.loadRecords(
            onSuccess = { records ->
                val monthlyRecords = ReportAnalyzer.getThisMonthRecords(records)
                report = ReportAnalyzer.analyze(monthlyRecords)

                bindBasicInfo()
                requestGptReAnalysis()
            },
            onFailure = {
                bindBasicInfo()
                requestGptReAnalysis()
            }
        )
    }

    private fun bindBasicInfo() {
        tvChipCategory.text = record.category
        tvChipStyle.text = report?.styleTitle ?: "분석 중"
        tvChipReview.text = if (review.hasReview()) "회고 완료" else "회고 없음"

        tvStateSummary.text = """
${record.category} 선택을 현재 시점에서 다시 살펴볼게요.
타임캡슐 회고와 월간 결정 패턴을 함께 반영합니다.
        """.trimIndent()
        tvDecisionSummary.text = buildCardText(
            "🧩 그때의 결정",
            """
${record.title.ifBlank { "제목 없음" }}

최종 선택
${record.selectedOption.ifBlank { "없음" }}

고민한 선택지
${record.choiceOptions.ifBlank { "없음" }}

선택 이유
${record.reason.ifBlank { "없음" }}

감정 점수
${record.emotionScore}점

당시 날씨
${record.weather}, ${record.temperature}℃, 습도 ${record.humidity}%
    """.trimIndent()
        )

        tvReviewSummary.text = buildCardText(
            "📮 타임캡슐 회고",
            if (review.hasReview()) {
                """
만족도 | ${review.satisfaction}/5
실제 결과 | ${review.result.ifBlank { "없음" }}
후회 정도 | ${review.regret.ifBlank { "없음" }}
다시 선택한다면 | ${review.retryChoice.ifBlank { "없음" }}
회고 메모 | ${review.memo.ifBlank { "없음" }}
        """.trimIndent()
            } else {
                "아직 저장된 회고가 없어요.\n회고를 입력하면 재분석이 더 정확해져요."
            }
        )

        val r = report

        tvPatternSummary.text = buildCardText(
            "📊 나의 월간 패턴",
            if (r != null) {
                """
결정 스타일 | ${r.styleTitle}
이번 달 결정 | ${r.totalCount}개
평균 만족도 | ${String.format("%.1f", r.avgSatisfaction)}점
평균 후회율 | ${r.avgRegretRate}%
최다 카테고리 | ${r.mostCategory}
결정 건강도 | ${r.healthScore}점
        """.trimIndent()
            } else {
                "월간 통계를 불러오지 못했어요.\n현재 기록과 회고만 기준으로 분석합니다."
            }
        )
    }

    private fun requestGptReAnalysis() {
        val apiKey = BuildConfig.OPENAI_API_KEY

        if (apiKey.isBlank()) {
            showFallbackReAnalysis()
            Toast.makeText(this, "API 키가 없어 기본 재분석을 표시합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        btnRetry.isEnabled = false
        btnRetry.text = "분석 중..."
        tvReAnalysisResult.text = "AI가 현재 시점에서 다시 분석하고 있어요."

        lifecycleScope.launch {
            try {
                val request = OpenAiChatRequest(
                    model = "gpt-4o-mini",
                    max_tokens = 750,
                    temperature = 0.65,
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content = """
너는 사용자의 과거 결정과 타임캡슐 회고를 바탕으로 현재 시점의 재분석을 해주는 한국어 AI야.
사용자를 평가하거나 단정하지 말고 부드럽게 말해.
과거의 선택, 실제 결과, 후회 정도, 월간 통계를 연결해서 분석해.
마크다운 기호 *, #, - 는 사용하지 마.
결과는 너무 길지 않게 작성해.
                            """.trimIndent()
                        ),
                        OpenAiMessage(
                            role = "user",
                            content = createReAnalysisPrompt()
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
                    showFallbackReAnalysis()
                } else {
                    tvReAnalysisResult.text = buildAnalysisResult(result)
                    saveReAnalysis(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showFallbackReAnalysis()
                Toast.makeText(this@ReAnalysisActivity, "AI 재분석에 실패해 기본 분석을 표시합니다.", Toast.LENGTH_SHORT).show()
            } finally {
                btnRetry.isEnabled = true
                btnRetry.text = "AI 재분석 다시 하기"
            }
        }
    }

    private fun createReAnalysisPrompt(): String {
        val r = report

        return """
다음은 사용자의 과거 결정 기록, 타임캡슐 회고, 월간 통계입니다.
이 선택을 현재 시점에서 다시 분석해주세요.

[과거 결정]
제목: ${record.title}
카테고리: ${record.category}
고민한 선택지: ${record.choiceOptions}
최종 선택: ${record.selectedOption}
선택 이유: ${record.reason}
기대 결과: ${record.expectedResult}
감정 점수: ${record.emotionScore}
결정 날짜: ${formatTimestamp(record.createdAt)}
날씨: ${record.weather}
기온: ${record.temperature}℃
습도: ${record.humidity}%
체감 상태: ${record.discomfort}

[타임캡슐 회고]
만족도: ${if (review.hasReview()) "${review.satisfaction}/5" else "회고 없음"}
실제 결과: ${review.result}
후회 정도: ${review.regret}
다시 선택 여부: ${review.retryChoice}
회고 메모: ${review.memo}

[사용자 월간 통계]
결정 스타일: ${r?.styleTitle ?: "통계 없음"}
이번 달 결정 수: ${r?.totalCount ?: 0}
평균 만족도: ${if (r != null) String.format("%.1f", r.avgSatisfaction) else "0.0"}
평균 후회율: ${r?.avgRegretRate ?: 0}%
가장 많이 고민한 카테고리: ${r?.mostCategory ?: "없음"}
결정 건강도: ${r?.healthScore ?: 0}점

[출력 형식]
아래 4개 제목을 그대로 사용해서 작성해주세요.

🔍 그때의 선택 다시 보기

과거 선택이 어떤 성격의 결정이었는지 2~3문장으로 설명해주세요.

📝 회고를 통해 보이는 점

실제 결과, 만족도, 후회 정도를 바탕으로 지금 알 수 있는 점을 설명해주세요.

📊 나의 결정 패턴과 연결

월간 통계와 결정 스타일을 이 선택과 연결해서 설명해주세요.

💡 다음 선택을 위한 조언

앞으로 비슷한 선택을 할 때 도움이 되는 구체적인 조언을 2~3문장으로 작성해주세요.
        """.trimIndent()
    }

    private fun saveReAnalysis(text: String) {
        val data = mapOf(
            "reAnalysisText" to text,
            "reAnalysisCreatedAt" to Timestamp.now()
        )

        db.collection("records")
            .document(recordId)
            .update(data)

        val targetReviewId = reviewDocId ?: return

        db.collection("records")
            .document(recordId)
            .collection("reviews")
            .document(targetReviewId)
            .update(data)
    }

    private fun showFallbackReAnalysis() {
        val fallback = """
🔍 그때의 선택 다시 보기

이 결정은 ${record.category} 안에서 사용자가 당시 상황과 감정 상태를 바탕으로 내린 선택으로 보여요. 선택 이유와 감정 점수를 함께 보면, 단순한 충동보다는 그때의 필요가 반영된 결정에 가까워요.

📝 회고를 통해 보이는 점

만족도는 ${review.satisfaction}/5이고, 후회 정도는 '${review.regret.ifBlank { "기록 없음" }}'로 기록되어 있어요. 실제 결과와 회고 메모를 보면 이 선택이 이후에 어떤 영향을 줬는지 다시 확인할 수 있어요.

📊 나의 결정 패턴과 연결

이번 달 결정 스타일은 '${report?.styleTitle ?: "분석 준비중"}'에 가까워요. 이 선택도 월간 패턴과 비교하면 나의 선택 방식이 어떤 방향으로 반복되는지 살펴볼 수 있어요.

💡 다음 선택을 위한 조언

비슷한 선택을 다시 해야 한다면, 선택 이유와 기대 결과를 한 번 더 구체적으로 적어보는 것이 좋아요. 특히 감정이 큰 날에는 바로 결정하기보다 잠깐 시간을 두고 다시 확인해보면 후회를 줄일 수 있어요.
""".trimIndent()

        tvReAnalysisResult.text = buildAnalysisResult(fallback)
        saveReAnalysis(fallback)
        btnRetry.isEnabled = true
        btnRetry.text = "AI 재분석 다시 하기"
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

        val bodyStart = title.length + 2
        val bodyLines = body.split("\n")
        var cursor = bodyStart

        bodyLines.forEach { line ->
            val trimmed = line.trim()

            if (
                trimmed in listOf(
                    "최종 선택",
                    "고민한 선택지",
                    "선택 이유",
                    "감정 점수",
                    "당시 날씨",
                    "만족도",
                    "실제 결과",
                    "후회 정도",
                    "다시 선택한다면",
                    "회고 메모",
                    "결정 스타일",
                    "이번 달 결정",
                    "평균 만족도",
                    "평균 후회율",
                    "최다 카테고리",
                    "결정 건강도"
                )
            ) {
                val start = fullText.indexOf(line, cursor)
                if (start >= 0) {
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        start + line.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            cursor += line.length + 1
        }

        return spannable
    }

    private fun buildAnalysisResult(text: String): SpannableString {
        val title = "AI 재분석 결과"
        val fullText = "$title\n\n$text"
        val spannable = SpannableString(fullText)

        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            title.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val headers = listOf(
            "🔍 그때의 선택 다시 보기",
            "📝 회고를 통해 보이는 점",
            "📊 나의 결정 패턴과 연결",
            "💡 다음 선택을 위한 조언"
        )

        headers.forEach { header ->
            val start = fullText.indexOf(header)
            if (start >= 0) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    start + header.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return spannable
    }

    private fun cleanCategory(category: String): String {
        return category
            .replace(Regex("[^가-힣a-zA-Z0-9\\s]"), "")
            .trim()
            .ifBlank { "기타" }
    }

    private fun formatTimestamp(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "알 수 없음"
        return SimpleDateFormat("yyyy. M. d. HH:mm", Locale.KOREA).format(date)
    }
}

data class ReAnalysisRecord(
    val title: String = "",
    val category: String = "",
    val choiceOptions: String = "",
    val selectedOption: String = "",
    val reason: String = "",
    val expectedResult: String = "",
    val emotionScore: Int = 0,
    val weather: String = "",
    val temperature: Double = 0.0,
    val humidity: Int = 0,
    val discomfort: String = "",
    val createdAt: Timestamp? = null
)

data class ReAnalysisReview(
    val satisfaction: Int = 0,
    val result: String = "",
    val regret: String = "",
    val retryChoice: String = "",
    val memo: String = "",
    val previousReAnalysis: String = ""
) {
    fun hasReview(): Boolean {
        return satisfaction > 0 || result.isNotBlank() || regret.isNotBlank() || memo.isNotBlank()
    }
}