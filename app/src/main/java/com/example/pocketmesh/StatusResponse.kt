package com.pocketmesh.app

data class StatusResponse(
    val server: String = "PocketMesh",
    val status: String = "running",
    val uptime_ms: Long,
    val ip: String?,
    val port: Int,
    val device: String,
    val android_version: String,
    val local_only: Boolean = true,
    val total_requests: Int
)