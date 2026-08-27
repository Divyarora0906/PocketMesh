package com.pocketmesh.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PocketDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1E2E),
    onPrimaryContainer = Color(0xFFC6D0F5),
    surface = Color(0xFF181825),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF11111B),
    onSurfaceVariant = Color(0xFFA6ADC8),
    background = Color(0xFF11111B),
    onBackground = Color(0xFFCDD6F4),
    error = Color(0xFFF38BA8),
    errorContainer = Color(0xFF313244)
)

sealed class QwenModelStatus {
    object Unloaded : QwenModelStatus()
    object Loading : QwenModelStatus()
    object Ready : QwenModelStatus()
    data class Generating(val prompt: String) : QwenModelStatus()
    data class Error(val message: String) : QwenModelStatus()
}

data class MeshRequestLog(
    val timestamp: String,
    val clientIp: String,
    val route: String,
    val latencyMs: Long,
    val status: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = PocketDarkColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PocketMeshNodeScreen(
                        onStartServer = { Thread { ServerManager.startServer() }.start() },
                        onStopServer = { Thread { ServerManager.stopServer() }.start() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketMeshNodeScreen(
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Server State
    val serverStatus by ServerManager.status.collectAsState()
    val ipAddress by ServerManager.ipAddress.collectAsState()
    val isServerRunning = serverStatus is ServerStatus.Running

    // Mesh Metrics State
    var connectedNodesCount by remember { mutableStateOf(1) } // Starts at 1 (your laptop/peer)
    var totalRequestsCount by remember { mutableStateOf(0) }
    var lastRoute by remember { mutableStateOf("/none") }
    var lastClientIp by remember { mutableStateOf("None") }
    var lastLatencyMs by remember { mutableStateOf(0L) }

    // Engine State
    val isEngineLoaded by LlamaEngine.isLoaded.collectAsState()
    var modelStatus by remember { mutableStateOf<QwenModelStatus>(QwenModelStatus.Unloaded) }
    var testPrompt by remember { mutableStateOf("Hi") }

    // Hardware Metrics State (RAM & Temp)
    var freeRamMb by remember { mutableStateOf(0L) }
    var totalRamMb by remember { mutableStateOf(0L) }
    var deviceTempC by remember { mutableStateOf(0.0f) }

    fun updateHardwareStats() {
        // RAM Stats
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        freeRamMb = memInfo.availMem / (1024 * 1024)
        totalRamMb = memInfo.totalMem / (1024 * 1024)

        // Device Thermal / Battery Temp Stats
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        deviceTempC = rawTemp / 10.0f
    }

    LaunchedEffect(Unit) {
        updateHardwareStats()
    }

    // Activity Logs & Structured Request Logs
    val logs = remember { mutableStateListOf("PocketMesh node initialized. Listening for peer requests.") }
    val requestHistory = remember { mutableStateListOf<MeshRequestLog>() }

    fun addLog(msg: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, "[$timestamp] $msg")
    }

    // Function to record incoming requests from external peers (Laptop, etc.)
    fun recordRequest(clientIp: String, route: String, latencyMs: Long, statusCode: Int = 200) {
        totalRequestsCount++
        lastClientIp = clientIp
        lastRoute = route
        lastLatencyMs = latencyMs

        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        requestHistory.add(0, MeshRequestLog(timestamp, clientIp, route, latencyMs, statusCode))
        addLog("HTTP $route hit from IP: $clientIp (${latencyMs}ms)")
        updateHardwareStats()
    }

    // --- HOOK INTO SERVER MANAGER ---
    // If your ServerManager exposes a Flow or callback for incoming requests, collect it here.
    // Example:
    // LaunchedEffect(Unit) {
    //     ServerManager.requestFlow.collect { req ->
    //         recordRequest(req.ip, req.route, req.latency)
    //     }
    // }

    LaunchedEffect(isEngineLoaded) {
        if (isEngineLoaded && modelStatus !is QwenModelStatus.Generating) {
            modelStatus = QwenModelStatus.Ready
        } else if (!isEngineLoaded && modelStatus is QwenModelStatus.Ready) {
            modelStatus = QwenModelStatus.Unloaded
        }
    }

    LaunchedEffect(serverStatus) {
        when (val currentStatus = serverStatus) {
            is ServerStatus.Running -> addLog("PocketMesh Node Server online on port ${ServerManager.PORT}")
            is ServerStatus.Stopped -> addLog("PocketMesh Node Server offline.")
            is ServerStatus.Error -> addLog("SERVER ERROR: ${currentStatus.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PocketMesh • Node Monitor",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E2E))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ------------------ 1. HARDWARE & THERMAL DIAGNOSTICS ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Device Runtime Diagnostics", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF89B4FA))
                        TextButton(onClick = { updateHardwareStats() }) {
                            Text("Refresh Stats", fontSize = 11.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(label = "Free / Total RAM", value = "$freeRamMb MB / $totalRamMb MB")
                        InfoItem(label = "Thermal Temp", value = "$deviceTempC °C")
                        InfoItem(label = "CPU Threads", value = "4 Active")
                    }
                }
            }

            // ------------------ 2. SERVER & PEER STATUS ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (isServerRunning) Color(0xFFA6E3A1) else Color(0xFFF38BA8),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (isServerRunning) "SERVER ONLINE" else "SERVER OFFLINE",
                                fontWeight = FontWeight.Bold,
                                color = if (isServerRunning) Color(0xFFA6E3A1) else Color(0xFFF38BA8),
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { if (isServerRunning) onStopServer() else onStartServer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServerRunning) Color(0xFFF38BA8) else Color(0xFF6366F1)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(if (isServerRunning) "Stop" else "Start", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(label = "Phone IP", value = ipAddress ?: "Not Connected")
                        InfoItem(label = "Port", value = ServerManager.PORT.toString())
                        InfoItem(label = "Connected Nodes", value = "$connectedNodesCount peer(s)")
                    }
                }
            }

            // ------------------ 3. TELEMETRY & LAST REQUEST METRICS ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Last Inbound Request Telemetry", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF89B4FA))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(label = "Total Requests", value = "$totalRequestsCount")
                        InfoItem(label = "Last Route", value = lastRoute)
                        InfoItem(label = "Last Latency", value = "${lastLatencyMs}ms")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(label = "Requesting Device IP", value = lastClientIp)
                        InfoItem(label = "Engine State", value = if (isEngineLoaded) "Loaded" else "Unloaded")
                    }
                }
            }

            // ------------------ 4. QWEN MODEL ENGINE CONTROL ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Qwen 0.5B AI Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text(
                                text = when (modelStatus) {
                                    is QwenModelStatus.Unloaded -> "Status: Unloaded"
                                    is QwenModelStatus.Loading -> "Status: Loading weights..."
                                    is QwenModelStatus.Ready -> "Status: Active & Ready"
                                    is QwenModelStatus.Generating -> "Status: Inferring response..."
                                    is QwenModelStatus.Error -> "Status: Load Failed"
                                },
                                fontSize = 11.sp,
                                color = when (modelStatus) {
                                    is QwenModelStatus.Ready -> Color(0xFFA6E3A1)
                                    is QwenModelStatus.Loading, is QwenModelStatus.Generating -> Color(0xFFFAB387)
                                    is QwenModelStatus.Error -> Color(0xFFF38BA8)
                                    else -> Color(0xFFA6ADC8)
                                }
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    if (isEngineLoaded) {
                                        LlamaEngine.unload()
                                        modelStatus = QwenModelStatus.Unloaded
                                        addLog("Unloaded Qwen model from memory.")
                                    } else {
                                        modelStatus = QwenModelStatus.Loading
                                        addLog("Searching for Qwen GGUF model...")

                                        val filesDir = context.getExternalFilesDir(null)
                                        val publicDownloadDir = File("/storage/emulated/0/Download")
                                        val targetFile = filesDir?.listFiles()?.firstOrNull { it.name.endsWith(".gguf", ignoreCase = true) }
                                            ?: publicDownloadDir.listFiles()?.firstOrNull { it.name.endsWith(".gguf", ignoreCase = true) }

                                        if (targetFile == null) {
                                            modelStatus = QwenModelStatus.Error("GGUF file missing")
                                            addLog("ERROR: Place any .gguf file in Downloads/")
                                            return@launch
                                        }

                                        val loaded = LlamaEngine.loadModel(targetFile.absolutePath)
                                        if (loaded) {
                                            modelStatus = QwenModelStatus.Ready
                                            addLog("Qwen 0.5B loaded successfully!")
                                            updateHardwareStats()
                                        } else {
                                            modelStatus = QwenModelStatus.Error("Native load failed")
                                            addLog("ERROR: Native loadModelNative returned false.")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEngineLoaded) Color(0xFFF38BA8) else Color(0xFFA6E3A1)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isEngineLoaded) "Unload" else "Run Qwen",
                                color = Color(0xFF11111B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (isEngineLoaded) {
                        HorizontalDivider(color = Color(0xFF313244))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = testPrompt,
                                onValueChange = { testPrompt = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Local Test Prompt", fontSize = 10.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFF313244)
                                )
                            )
                            Button(
                                onClick = {
                                    if (testPrompt.isBlank()) return@Button
                                    coroutineScope.launch {
                                        val startTime = System.currentTimeMillis()
                                        modelStatus = QwenModelStatus.Generating(testPrompt)
                                        addLog("Local prompt inference started...")

                                        val response = withContext(Dispatchers.IO) {
                                            LlamaEngine.generate(testPrompt)
                                        }

                                        val latency = System.currentTimeMillis() - startTime
                                        recordRequest("127.0.0.1 (Local)", "/chat", latency)
                                        modelStatus = QwenModelStatus.Ready
                                        updateHardwareStats()
                                    }
                                },
                                enabled = modelStatus is QwenModelStatus.Ready,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ------------------ 5. RECENT REQUEST ACTIVITY LOG ------------------
            Text("Recent Inbound Requests", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color(0xFF313244), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(requestHistory) { req ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[${req.timestamp}] Route: ${req.route} | IP: ${req.clientIp}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFA6E3A1)
                            )
                            Text(
                                text = "${req.latencyMs}ms",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFF89B4FA)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 9.sp, color = Color(0xFFA6ADC8))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}