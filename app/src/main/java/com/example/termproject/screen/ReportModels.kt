package com.example.termproject.screen

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class ReportRecord(
    val id: String,
    val title: String,
    val category: String,
    val emotionScore: Int,
    val createdAt: Timestamp?,
    val reviewCompleted: Boolean,
    val satisfaction: Int?,
    val regret: String?
)

data class ReportResult(
    val monthTitle: String,
    val totalCount: Int,
    val reviewCompletedCount: Int,
    val avgSatisfaction: Double,
    val avgRegretRate: Int,
    val healthScore: Int,
    val styleTitle: String,
    val styleSummary: String,
    val recommendText: String,
    val heroSummary: String,
    val mostCategory: String,
    val mostCategoryCount: Int,
    val highRegretCategory: String,
    val highRegretRate: Int,
    val avgEmotionScore: Int,
    val daySatisfaction: Int,
    val nightSatisfaction: Int,
    val categoryCounts: List<Pair<String, Int>>,
    val weeklyCounts: List<Pair<String, Int>>,
    val emotionSatisfaction: List<Pair<String, Int>>,
    val topSatisfied: List<ReportRecord>,
    val topRegretted: List<ReportRecord>
)

object ReportRepository {
    private val db = FirebaseFirestore.getInstance()

    fun loadRecords(
        onSuccess: (List<ReportRecord>) -> Unit,
        onFailure: () -> Unit
    ) {
        db.collection("records")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val docs = result.documents

                if (docs.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                val reviewTasks = docs.map { doc ->
                    doc.reference
                        .collection("reviews")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                }

                Tasks.whenAllSuccess<QuerySnapshot>(reviewTasks)
                    .addOnSuccessListener { reviewSnapshots ->
                        val records = docs.mapIndexed { index, doc ->
                            val reviewDoc = reviewSnapshots[index].documents.firstOrNull()

                            ReportRecord(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                category = doc.getString("category") ?: "기타",
                                emotionScore = doc.getLong("emotionScore")?.toInt() ?: 0,
                                createdAt = doc.getTimestamp("createdAt"),
                                reviewCompleted = doc.getBoolean("reviewCompleted") ?: false,
                                satisfaction = reviewDoc?.getLong("satisfaction")?.toInt(),
                                regret = reviewDoc?.getString("regret")
                            )
                        }

                        onSuccess(records)
                    }
                    .addOnFailureListener { onFailure() }
            }
            .addOnFailureListener { onFailure() }
    }
}

object ReportAnalyzer {

    fun getThisMonthRecords(records: List<ReportRecord>): List<ReportRecord> {
        val now = Calendar.getInstance()

        return records.filter { record ->
            val date = record.createdAt?.toDate() ?: return@filter false
            val cal = Calendar.getInstance().apply { time = date }

            now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    now.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        }
    }

    fun analyze(records: List<ReportRecord>): ReportResult {
        val reviewed = records.filter { it.satisfaction != null || it.regret != null }

        val monthTitle = SimpleDateFormat("M월", Locale.KOREA).format(Calendar.getInstance().time)

        val categoryCounts = records
            .groupingBy { it.category.ifBlank { "기타" } }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

        val mostCategoryPair = categoryCounts.firstOrNull()

        val highRegretPair = reviewed
            .groupBy { it.category.ifBlank { "기타" } }
            .mapValues { entry ->
                entry.value.map { regretToScore(it.regret) }.averageOrZero().roundToInt()
            }
            .toList()
            .sortedByDescending { it.second }
            .firstOrNull()

        val avgEmotion = records.map { it.emotionScore }.averageOrZero().roundToInt()
        val avgSatisfaction = reviewed.mapNotNull { it.satisfaction }.averageOrZero()
        val avgRegret = reviewed.map { regretToScore(it.regret) }.averageOrZero().roundToInt()

        val reviewRate = if (records.isEmpty()) 0 else (reviewed.size * 100.0 / records.size).roundToInt()

        val healthScore = (
                (avgSatisfaction / 5.0 * 45.0) +
                        ((100 - avgRegret) * 0.35) +
                        (reviewRate * 0.20)
                ).roundToInt().coerceIn(0, 100)

        val dayRecords = reviewed.filter { isDay(it) }
        val nightRecords = reviewed.filter { !isDay(it) }

        val daySatisfaction = dayRecords.mapNotNull { it.satisfaction }.averageOrZero().roundToInt()
        val nightSatisfaction = nightRecords.mapNotNull { it.satisfaction }.averageOrZero().roundToInt()
        val nightRegret = nightRecords.map { regretToScore(it.regret) }.averageOrZero()

        val weeklyCounts = listOf(
            "1주차" to countWeek(records, 1),
            "2주차" to countWeek(records, 2),
            "3주차" to countWeek(records, 3),
            "4주차" to countWeek(records, 4),
            "5주차" to countWeek(records, 5)
        )

        val emotionSatisfaction = listOf(
            "감정 1~2" to avgSatisfactionByEmotion(reviewed, 1, 2),
            "감정 3~4" to avgSatisfactionByEmotion(reviewed, 3, 4),
            "감정 5~7" to avgSatisfactionByEmotion(reviewed, 5, 7)
        )

        val styleTitle = when {
            records.size < 3 -> "분석 준비중"
            avgRegret <= 30 && avgSatisfaction >= 4.0 -> "안전형"
            avgEmotion >= 5 && nightRegret >= 40 -> "감정형"
            avgRegret >= 60 && avgSatisfaction <= 3.0 -> "회피형"
            avgEmotion >= 5 && avgSatisfaction >= 3.5 -> "도전형"
            else -> "안전형"
        }

        val styleSummary = when (styleTitle) {
            "분석 준비중" -> "아직 이번 달 기록이 적어요. 결정을 조금 더 기록하면 더 정확한 분석이 가능해요."
            "안전형" -> "후회율이 낮고 만족도가 안정적인 편이에요. 신중하게 고민한 뒤 결정하는 스타일이에요."
            "도전형" -> "새로운 선택을 시도하는 경향이 있어요. 감정 점수가 높아도 만족도가 비교적 잘 유지되는 편이에요."
            "감정형" -> "감정 점수가 높은 상태에서 내린 결정이 많아요. 중요한 결정은 잠시 시간을 두고 다시 확인해보면 좋아요."
            "회피형" -> "후회율이 높고 만족도가 낮은 결정이 나타나는 편이에요. 작은 선택부터 빠르게 정하는 연습이 도움이 돼요."
            else -> "아직 분석할 데이터가 부족해요."
        }

        val recommendText = when (styleTitle) {
            "감정형" -> "감정 점수가 5점 이상일 때는 바로 결정하지 말고, 30분 뒤 다시 확인해보세요."
            "회피형" -> "작은 결정은 제한 시간을 정해서 빠르게 선택하는 연습을 해보세요."
            "도전형" -> "새로운 선택을 하되, 결정 이유를 짧게 기록하면 회고할 때 더 도움이 돼요."
            "안전형" -> "현재처럼 결정 이유와 결과를 함께 기록하면 안정적인 패턴을 유지할 수 있어요."
            else -> "결정과 회고를 더 쌓으면 맞춤 추천이 더 정확해져요."
        }

        val heroSummary = if (records.isEmpty()) {
            "이번 달 기록된 결정이 아직 없어요.\n결정을 기록하면 월간 리포트가 자동으로 생성돼요."
        } else {
            "이번 달 ${records.size}개의 결정을 기록했어요.\n가장 많이 고민한 분야는 '${mostCategoryPair?.first ?: "기타"}'였고,\n평균 만족도는 ${String.format("%.1f", avgSatisfaction)}점이에요."
        }

        return ReportResult(
            monthTitle = monthTitle,
            totalCount = records.size,
            reviewCompletedCount = reviewed.size,
            avgSatisfaction = avgSatisfaction,
            avgRegretRate = avgRegret,
            healthScore = healthScore,
            styleTitle = styleTitle,
            styleSummary = styleSummary,
            recommendText = recommendText,
            heroSummary = heroSummary,
            mostCategory = mostCategoryPair?.first ?: "데이터 없음",
            mostCategoryCount = mostCategoryPair?.second ?: 0,
            highRegretCategory = highRegretPair?.first ?: "데이터 없음",
            highRegretRate = highRegretPair?.second ?: 0,
            avgEmotionScore = avgEmotion,
            daySatisfaction = daySatisfaction,
            nightSatisfaction = nightSatisfaction,
            categoryCounts = categoryCounts,
            weeklyCounts = weeklyCounts,
            emotionSatisfaction = emotionSatisfaction,
            topSatisfied = reviewed.sortedByDescending { it.satisfaction ?: 0 }.take(3),
            topRegretted = reviewed.sortedByDescending { regretToScore(it.regret) }.take(3)
        )
    }

    fun regretToScore(regret: String?): Int {
        return when (regret) {
            "많이" -> 100
            "조금" -> 50
            else -> 0
        }
    }

    private fun isDay(record: ReportRecord): Boolean {
        val date = record.createdAt?.toDate() ?: return true
        val cal = Calendar.getInstance().apply { time = date }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in 6..17
    }

    private fun countWeek(records: List<ReportRecord>, week: Int): Int {
        return records.count {
            val date = it.createdAt?.toDate() ?: return@count false
            val cal = Calendar.getInstance().apply { time = date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            ((day - 1) / 7 + 1) == week
        }
    }

    private fun avgSatisfactionByEmotion(records: List<ReportRecord>, min: Int, max: Int): Int {
        return records
            .filter { it.emotionScore in min..max }
            .mapNotNull { it.satisfaction }
            .averageOrZero()
            .roundToInt()
    }

    private fun List<Int>.averageOrZero(): Double {
        return if (isEmpty()) 0.0 else average()
    }
}