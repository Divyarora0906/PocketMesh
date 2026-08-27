package com.pocketmesh.app

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ServerStatus {
    object Stopped : ServerStatus()
    object Running : ServerStatus()
    data class Error(val message: String) : ServerStatus()
}

object ServerManager {
    private const val TAG = "PocketMesh"
    const val PORT = 8080

    private var server: PocketMeshServer? = null

    private val _status = MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private val _ipAddress = MutableStateFlow<String?>(null)
    val ipAddress: StateFlow<String?> = _ipAddress.asStateFlow()

    @Synchronized
    fun startServer() {
        if (server != null && server?.isAlive == true) {
            Log.d(TAG, "Server attempt ignored: Server is already running.")
            return
        }

        // Fetch IP (Wi-Fi or Hotspot). If null, proceed with fallback instead of blocking NanoHTTPD
        val detectedIp = NetworkUtils.getLocalIpAddress()
        val activeIp = detectedIp ?: "192.168.43.1" // Fallback default Hotspot IP

        try {
            Log.d(TAG, "Starting server on port $PORT with Qwen LlamaChatService...")

            // Instantiate PocketMeshServer with live LlamaChatService
            val llamaService = LlamaChatService()
            val newServer = PocketMeshServer(
                port = PORT,
                chatService = llamaService
            )

            newServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)

            server = newServer
            _ipAddress.value = activeIp
            _status.value = ServerStatus.Running
            Log.d(TAG, "Server started successfully at http://$activeIp:$PORT")
        } catch (e: Exception) {
            val errorMsg = "Failed to start server: ${e.message}"
            Log.e(TAG, errorMsg, e)
            stopServerInternal()
            _status.value = ServerStatus.Error(errorMsg)
        }
    }

    @Synchronized
    fun stopServer() {
        stopServerInternal()
        _status.value = ServerStatus.Stopped
    }

    private fun stopServerInternal() {
        try {
            if (server != null) {
                Log.d(TAG, "Stopping server...")
                server?.stop()
                server = null
                Log.d(TAG, "Server stopped.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while stopping server: ${e.message}", e)
        }
    }
}