package com.example.termproject.network

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val max_tokens: Int = 700,
    val temperature: Double = 0.7
)

data class OpenAiMessage(
    val role: String,
    val content: String
)

data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>
)

data class OpenAiChoice(
    val message: OpenAiMessage
)