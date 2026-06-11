package com.example.termproject.ml

import java.util.Calendar
import java.util.Locale

object DecisionFeatureExtractor {

    fun extract(
        category: String,
        choiceOptions: String,
        reason: String,
        emotionScore: Int,
        temperature: Double,
        humidity: Int,
        discomfortIndex: Double,
        createdTime: Long
    ): FloatArray {
        val categoryId = categoryToId(category).toFloat()
        val safeEmotionScore = emotionScore.coerceIn(1, 7).toFloat()
        val reasonLength = reason.length.toFloat()
        val choiceCount = countChoices(choiceOptions).toFloat()
        val hour = extractHour(createdTime).toFloat()

        return floatArrayOf(
            categoryId,
            safeEmotionScore,
            reasonLength,
            choiceCount,
            temperature.toFloat(),
            humidity.toFloat(),
            discomfortIndex.toFloat(),
            hour
        )
    }

    private fun categoryToId(category: String): Int {
        val normalized = category.trim().lowercase(Locale.KOREA)

        return when {
            normalized.contains("진로") -> 0
            normalized.contains("연애") -> 1
            normalized.contains("인간") || normalized.contains("관계") -> 2
            normalized.contains("소비") || normalized.contains("돈") -> 3
            normalized.contains("공부") || normalized.contains("학업") -> 4
            normalized.contains("업무") || normalized.contains("일") -> 5
            normalized.contains("음식") || normalized.contains("식사") -> 6
            else -> 7
        }
    }

    private fun countChoices(choiceOptions: String): Int {
        val cleaned = choiceOptions.trim()

        if (cleaned.isBlank()) {
            return 1
        }

        val parts = cleaned
            .split(",", "/", "\n", " vs ", "VS", "또는", "or")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return parts.size.coerceAtLeast(1)
    }

    private fun extractHour(createdTime: Long): Int {
        val calendar = Calendar.getInstance(Locale.KOREA)
        calendar.timeInMillis = createdTime
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
}