package com.pocketmesh.app

import android.util.Log

class LlamaChatService : ChatService {

    override fun processMessage(message: String): ChatResponse {
        val startTime = System.currentTimeMillis()
        return try {
            // Simplified plain prompt to test if the model responds without ChatML overhead
            val prompt = "User: $message\nAssistant:"

            Log.d("LlamaChatService", "Sending prompt to native engine: $prompt")

            val rawOutput = LlamaEngine.generateSync(prompt)
            Log.d("LlamaChatService", "Raw native output received: '$rawOutput'")

            val cleanOutput = rawOutput
                .replace("User:", "")
                .replace("Assistant:", "")
                .trim()

            val elapsedTime = System.currentTimeMillis() - startTime

            ChatResponse(
                response = if (cleanOutput.isBlank()) "(Model returned empty response)" else cleanOutput,
                model = "qwen-0.5b",
                status = "success",
                latency_ms = elapsedTime
            )
        } catch (e: Exception) {
            Log.e("LlamaChatService", "Inference error: ${e.message}", e)
            val elapsedTime = System.currentTimeMillis() - startTime

            ChatResponse(
                response = "Inference failed: ${e.localizedMessage}",
                model = "qwen-0.5b",
                status = "error",
                latency_ms = elapsedTime
            )
        }
    }
}