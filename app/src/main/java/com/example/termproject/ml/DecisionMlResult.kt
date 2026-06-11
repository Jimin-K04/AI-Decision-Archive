package com.example.termproject.ml

data class DecisionMlResult(
    val regretLabel: String,
    val confidence: Float,
    val probabilities: FloatArray
)