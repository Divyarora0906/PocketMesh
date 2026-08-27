package com.pocketmesh.app
import android.os.Build
import android.util.Log
import com.pocketmesh.app.StatusResponse
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.atomic.AtomicInteger

class PocketMeshServer(
    private val port: Int = 8080,
    private val chatService: ChatService = MockChatService()
) : NanoHTTPD("0.0.0.0", port) {

    private val gson = Gson()
    private val serverStartTimeMs = System.currentTimeMillis()
    private val requestCounter = AtomicInteger(0)

    data class HealthResponse(
        val status: String = "ok",
        val server: String = "PocketMesh",
        val source: String = "local"
    )

    override fun serve(session: IHTTPSession): Response {
        val totalRequests = requestCounter.incrementAndGet()
        val uri = session.uri
        val method = session.method

        Log.d("PocketMesh", "Request #$totalRequests: ${method.name} $uri")

        // Handle CORS preflight OPTIONS requests
        if (method == Method.OPTIONS) {
            return createCorsResponse(Response.Status.OK, "text/plain", "OK")
        }

        return when (uri) {
            "/", "/health" -> {
                if (method != Method.GET) return methodNotAllowedResponse()
                val jsonBody = gson.toJson(HealthResponse())
                createCorsResponse(Response.Status.OK, "application/json", jsonBody)
            }

            "/status" -> {
                if (method != Method.GET) return methodNotAllowedResponse()
                handleStatus()
            }

            "/chat" -> {
                if (method != Method.POST) return methodNotAllowedResponse()
                handleChat(session)
            }

            else -> {
                val errorBody = gson.toJson(mapOf("error" to "404 Not Found"))
                createCorsResponse(Response.Status.NOT_FOUND, "application/json", errorBody)
            }
        }
    }

    private fun handleStatus(): Response {
        val response = StatusResponse(
            uptime_ms = System.currentTimeMillis() - serverStartTimeMs,
            ip = NetworkUtils.getLocalIpAddress(),
            port = port,
            device = Build.MODEL,
            android_version = Build.VERSION.RELEASE,
            local_only = true,
            total_requests = requestCounter.get()
        )
        return createCorsResponse(Response.Status.OK, "application/json", gson.toJson(response))
    }

    private fun handleChat(session: IHTTPSession): Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val postData = files["postData"]

            if (postData.isNullOrEmpty()) {
                return badRequestResponse("Missing or empty request body")
            }

            val chatRequest = gson.fromJson(postData, ChatRequest::class.java)
            if (chatRequest?.message == null) {
                return badRequestResponse("Missing 'message' field in JSON payload")
            }

            val chatResponse = chatService.processMessage(chatRequest.message)
            createCorsResponse(
                Response.Status.OK,
                "application/json",
                gson.toJson(chatResponse)
            )
        } catch (e: Exception) {
            Log.e("PocketMesh", "Error handling /chat request: ${e.message}", e)
            badRequestResponse("Invalid JSON payload")
        }
    }

    private fun badRequestResponse(message: String): Response {
        val errorBody = gson.toJson(mapOf("error" to message))
        return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", errorBody)
    }

    private fun methodNotAllowedResponse(): Response {
        val errorBody = gson.toJson(mapOf("error" to "Method not allowed"))
        return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", errorBody)
    }

    private fun createCorsResponse(status: Response.IStatus, mimeType: String, message: String): Response {
        val response = newFixedLengthResponse(status, mimeType, message)
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
        return response
    }
}