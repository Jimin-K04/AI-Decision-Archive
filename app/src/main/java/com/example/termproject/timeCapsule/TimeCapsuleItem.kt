package com.example.termproject.timeCapsule

data class TimeCapsuleItem(
    val id: String = "",
    val title: String = "",
    val choiceOptions: String = "",
    val selectedOption: String = "",
    val reason: String = "",

    val category: String = "",
    val dateText: String = "",
    val discomfort: String = "",
    val emotionScore: Int = 0,
    val humidity: Int = 0,
    val temperature: Double = 0.0,
    val weather: String = "",
    val reviewCompleted: Boolean = false,
    val previousReviewText: String = ""
)

data class ReviewInput(
    val satisfaction: Int,
    val result: String,
    val regret: String,
    val retryChoice: String,
    val memo: String
)