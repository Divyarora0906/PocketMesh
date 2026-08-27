package com.pocketmesh.app

interface ChatService {
    fun processMessage(message: String): ChatResponse
}

data class ChatResponse(
    val response: String,
    val model: String = "qwen-0.5b",
    val status: String = "success",
    val latency_ms: Long = 0L
)

class MockChatService : ChatService {
    override fun processMessage(message: String): ChatResponse {
        return ChatResponse(
            response = "Mock echo: $message",
            model = "mock-model",
            status = "success",
            latency_ms = 10L
        )
    }
}