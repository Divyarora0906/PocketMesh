# PocketMesh ⚡

### Turn smartphones into portable Edge-AI infrastructure.

> *Computing should not always travel to the cloud.  
> Sometimes the infrastructure can travel with you.*

PocketMesh transforms an Android smartphone from a passive AI client into an **active, local AI compute node** — capable of running LLM inference on-device, exposing a local HTTP API, and laying the groundwork for a future device-to-device AI mesh network. Zero cloud. Zero subscription. Just the hardware already in your pocket.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API_26+-3DDC84?logo=android&logoColor=white)
![C++17](https://img.shields.io/badge/C++-17-00599C?logo=cplusplus&logoColor=white)
![llama.cpp](https://img.shields.io/badge/llama.cpp-native-orange)
![GGUF](https://img.shields.io/badge/GGUF-model_format-blue)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)
![iQOO 15](https://img.shields.io/badge/device-iQOO%2015-red)
![Snapdragon](https://img.shields.io/badge/chipset-Snapdragon%208%20Elite%20Gen%205-1A80DA)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 📋 Table of Contents

- [What is PocketMesh?](#-what-is-pocketmesh)
- [iQOO Hackathon](#-iqoo-hackathon)
- [The Problem](#-the-problem)
- [The PocketMesh Approach](#-the-pocketmesh-approach)
- [Why This Is Different](#-why-this-is-different)
- [Current Capabilities](#-current-capabilities)
- [Architecture](#-architecture)
- [End-to-End Data Flow](#-end-to-end-data-flow)
- [Technical Stack](#-technical-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Local API Documentation](#-local-api-documentation)
- [Security Model](#-security-model)
- [Performance Considerations](#-performance-considerations)
- [Current Implementation Status](#-current-implementation-status)
- [Limitations](#-limitations)
- [Engineering Challenges](#-engineering-challenges)
- [Roadmap](#-roadmap)
- [Future Distributed Architecture](#-future-distributed-architecture)
- [Use Cases](#-use-cases)
- [Design Principles](#-design-principles)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🧠 What is PocketMesh?

PocketMesh is an **Edge-AI micro-infrastructure platform** for Android. It runs a quantized LLM (Qwen 0.5B) entirely on-device using **llama.cpp** through a **JNI/C++ bridge**, and exposes inference capabilities over a **local HTTP API** — making your phone an addressable AI node on the local network.

**Today:** A single phone runs local AI and serves it over HTTP.  
**Tomorrow:** Multiple phones discover each other, share capabilities, and form a cooperative AI mesh.

```
Smartphone  →  Local AI Node  →  Network Node  →  Collaborative AI Mesh
```

---

## 📱 iQOO Hackathon

PocketMesh is being built and demoed for the **iQOO x Reskilll Hackathon** on the **iQOO 15**, running the **Snapdragon 8 Elite Gen 5** — a chip purpose-built for exactly this kind of on-device AI workload. This isn't incidental hardware; the whole architecture leans on what this chip and this device make possible.

### Why the iQOO 15 specifically

- **Snapdragon 8 Elite Gen 5** — flagship-class CPU cores plus a real NPU, meaning the on-device inference this project depends on can actually run at demoable speed, not just in theory
- **Enough RAM/storage headroom** to keep a quantized GGUF model resident in memory alongside the OS and UI without constant reload pressure

### What we're building at the hackathon

| Feature | What it does |
|---|---|
| 🧠 **NPU-accelerated inference (ExecuTorch integration)** | Move inference off pure CPU threads and onto the Snapdragon 8 Elite Gen 5's NPU via ExecuTorch, for faster and more power-efficient on-device generation |
| 📚 **Offline RAG — document upload + on-device vector search** | Upload a document to the phone, embed and index it locally, and answer questions grounded in it — entirely offline |
| 🛡️ **Anti-tamper security layer (IMU + camera)** | Use the phone's IMU and camera as a lightweight tamper/presence-detection signal, flagging unexpected movement or handling of the device |
| 📡 **Office Kit-connected live telemetry dashboard** | Use iQOO's Office Kit bridge so a connected laptop shows real-time server/device telemetry pulled directly from the phone, not simulated data |

### MVP being built this week

- **Phone-hosted local server** — hotspot ON → a lightweight server spins up and becomes reachable on the local network
- **Local API loop** — a laptop connects, sends a real request, and gets a real response back — **zero internet** involved at any point
- **On-device model (stretch goal)** — a small quantized model (Qwen 0.5B / TinyLlama) running directly on the phone, so the response in that loop is genuinely AI-generated

### Demo

Live, in front of the judges: hotspot on, laptop connects to the phone over local Wi-Fi, sends a real chat request, and gets back a response generated entirely on-device — no cloud call, no internet connection, no simulated data. What's on screen is exactly what's happening on the hardware.

---

## 🔥 The Problem

Cloud-first AI creates hard dependencies:

| Limitation | Impact |
|---|---|
| **Internet Required** | No connectivity = no AI |
| **Latency** | Round-trip to distant data centers |
| **Privacy Risk** | Every prompt leaves your device |
| **Centralized Control** | Single provider, single point of failure |
| **Recurring Cost** | Pay-per-token, per-month, per-seat |
| **Connectivity Failures** | Airplane, rural, disaster, underground |

```
        Cloud AI                    PocketMesh

     User                        User
      ↓                           ↓
     Internet                    Phone
      ↓                           ↓
     Cloud                      Local Model
      ↓                           ↓
     GPU Cluster                 Response
      ↓                        (no internet needed)
     Response
  (requires internet)
```

**PocketMesh takes the opposite approach:** the model lives where the user is.

---

## 🎯 The PocketMesh Approach

A modern smartphone already contains the building blocks of a compute node:

| Resource | Already in Your Pocket |
|---|---|
| CPU | Multi-core ARM (Cortex-A7x) |
| RAM | 4–16 GB |
| Storage | 64–512 GB |
| Battery | Self-powered for hours |
| Networking | Wi-Fi, Hotspot, Bluetooth, NFC |
| Sensors | GPS, accelerometer, gyroscope |

PocketMesh does not add new hardware. It **repurposes what already exists** — turning a consumer device into programmable AI infrastructure.

---

## 💡 Why This Is Different

Most mobile AI apps are thin clients:

```
Traditional Mobile AI:    Phone ───→ Cloud API ───→ Response
```

PocketMesh makes the phone itself the server:

```
PocketMesh (Today):       Phone ═══ AI Node ═══ HTTP API ═══ Any LAN Client
```

The key shift is not just *running* a model locally, but making the device:

- **Addressable** — other devices can reach it via IP:port
- **Computationally useful** — it serves inference, not just consumes it
- **Network-aware** — detects and exposes its local IP (Wi-Fi / Hotspot)
- **Capability-aware** — monitors RAM, temperature, battery in real-time
- **Eventually cooperative** — architecture designed toward multi-node mesh *(future)*

```
Future PocketMesh:        Phone ←──→ Phone ←──→ Phone
                          (mesh of local AI nodes)
```

---

## ✅ Current Capabilities

What PocketMesh **actually does today** (verified from codebase):

- 🧠 **On-device LLM inference** — Runs quantized GGUF models (Qwen 0.5B) natively via llama.cpp
- 🌐 **Local HTTP API server** — NanoHTTPD server on port 8080 serving `/health`, `/status`, `/chat`
- 📱 **Node Monitor UI** — Jetpack Compose dashboard showing server state, hardware metrics, request logs
- ⚡ **JNI/C++ native bridge** — Kotlin → JNI → C++ → llama.cpp pipeline for direct hardware-level inference
- 🌡️ **Real-time diagnostics** — RAM usage, battery temperature, request telemetry displayed in UI
- 📡 **Network detection** — Automatic Wi-Fi / Hotspot IP discovery (RFC 1918 private subnets)
- 🎛️ **Model lifecycle management** — Load, generate, unload models with full state tracking
- 💬 **ChatML prompt formatting** — Qwen-compatible `<|im_start|>` / `<|im_end|>` template in native layer
- 🔄 **CORS-enabled API** — Cross-origin requests allowed for browser/frontend integration
- 📊 **Request tracking** — Atomic request counter, per-request latency measurement, client IP logging

---

## 🏗️ Architecture

### Current System Architecture

```
                    POCKETMESH NODE (Android)
┌──────────────────────────────────────────────────┐
│                                                  │
│   ┌──────────────────────────────────────────┐   │
│   │         Jetpack Compose UI               │   │
│   │   (Node Monitor + Diagnostics Panel)     │   │
│   └─────────────────┬────────────────────────┘   │
│                     │                            │
│         ┌───────────┴───────────┐                │
│         ▼                       ▼                │
│   ┌───────────┐         ┌─────────────┐         │
│   │  Server   │         │   Llama     │         │
│   │  Manager  │         │   Engine    │         │
│   │ (NanoHTTP)│         │ (StateFlow) │         │
│   └─────┬─────┘         └──────┬──────┘         │
│         │                      │                │
│         ▼                      │                │
│   ┌───────────────┐            │                │
│   │ PocketMesh    │            │                │
│   │ Server        │            │                │
│   │ (Routes)      │            │                │
│   └─────┬─────────┘            │                │
│         │                      │                │
│         ▼                      ▼                │
│   ┌───────────────┐    ┌──────────────┐         │
│   │ LlamaChat     │───▶│  JNI Bridge  │         │
│   │ Service       │    │  (native)    │         │
│   └───────────────┘    └──────┬───────┘         │
│                               │                 │
│                               ▼                 │
│                        ┌──────────────┐         │
│                        │  llama.cpp   │         │
│                        │  (C++ lib)   │         │
│                        └──────┬───────┘         │
│                               │                 │
│                               ▼                 │
│                        ┌──────────────┐         │
│                        │  GGUF Model  │         │
│                        │ (on storage) │         │
│                        └──────────────┘         │
│                                                  │
│   ┌──────────────────────────────────────────┐   │
│   │          NetworkUtils                    │   │
│   │  (Wi-Fi / Hotspot IP Detection)          │   │
│   └──────────────────────────────────────────┘   │
│                                                  │
└──────────────────────────────────────────────────┘
```

### HTTP Request Flow

```
   External Client (laptop, browser, curl)
              │
              ▼
   ┌──────────────────┐
   │ NanoHTTPD Server │  0.0.0.0:8080
   │ (PocketMesh.kt)  │
   └────────┬─────────┘
            │
            ├── GET  /         → HealthResponse { status, server, source }
            ├── GET  /health   → HealthResponse { status, server, source }
            ├── GET  /status   → StatusResponse { uptime, ip, device, requests }
            ├── POST /chat     → ChatRequest → LlamaChatService → LlamaEngine
            ├── OPTIONS *      → CORS Preflight (200 OK)
            └── *   /*         → 404 Not Found
            │
            ▼
   ┌──────────────────┐
   │ LlamaChatService │
   │ (ChatService)    │
   └────────┬─────────┘
            │
            ▼
   ┌──────────────────┐
   │ LlamaEngine      │  @Synchronized
   │ .generateSync()  │
   └────────┬─────────┘
            │ JNI
            ▼
   ┌──────────────────┐
   │ PocketMesh-      │  C++17, -O3
   │ native.cpp       │
   └────────┬─────────┘
            │
            ▼
   ┌──────────────────┐
   │ llama.cpp        │  Tokenize → Decode → Sample → Detokenize
   │ (GGUF runtime)   │  Sampling: greedy (argmax), Max tokens: 96
   └────────┬─────────┘
            │
            ▼
   ┌──────────────────┐
   │ ChatResponse     │  { response, model, status, latency_ms }
   └──────────────────┘
```

---

## 🔄 End-to-End Data Flow

A complete inference request travels through 7 layers:

```
1. Client        curl -X POST http://192.168.x.x:8080/chat -d '{"message":"hello"}'
       ↓
2. NanoHTTPD     PocketMeshServer.serve() → route to /chat handler
       ↓
3. ChatService   LlamaChatService.processMessage("hello")
       ↓
4. LlamaEngine   LlamaEngine.generateSync("User: hello\nAssistant:")
       ↓ JNI
5. C++ Native    Java_com_pocketmesh_app_LlamaEngine_generateResponseNative()
       ↓         - Wraps in ChatML template (<|im_start|>system/user/assistant)
       ↓         - Tokenizes via llama_tokenize()
       ↓         - Evaluates prompt tokens via llama_decode()
       ↓
6. Generation    Greedy (argmax) sampling loop (up to 96 tokens)
       ↓         - Deterministic highest-logit token selection
       ↓         - EOG token check (llama_vocab_is_eog)
       ↓         - <|im_end|> stop marker detection (text-level fallback)
       ↓
7. Response      JSON: { "response": "...", "model": "qwen-0.5b", "latency_ms": 1234 }
```

> **Note:** Sampling is currently **greedy/deterministic** (always picks the highest-probability token), not temperature-based. This means identical prompts always produce identical output — good for reproducible demos, but more prone to repetitive phrasing than temperature/top-p sampling. See [Roadmap](#-roadmap) for planned sampling improvements.

---

## 🛠️ Technical Stack

| Layer | Technology | Purpose |
|---|---|---|
| **UI** | Jetpack Compose + Material3 | Node monitor dashboard |
| **Application** | Kotlin 2.0, Coroutines | Async model loading, state management |
| **HTTP Server** | NanoHTTPD 2.3.1 | Lightweight embedded HTTP server |
| **Serialization** | Gson 2.11.0 | JSON request/response handling |
| **JNI Bridge** | JNI (Java Native Interface) | Kotlin ↔ C++ boundary |
| **Native Runtime** | C++17, `-O3` optimized | High-performance inference host |
| **AI Engine** | llama.cpp (ggml-org) | GGUF model loading + inference |
| **Model Format** | GGUF (quantized) | Compact model storage for mobile |
| **Build System** | Gradle 8.13.2 + CMake 3.22.1 | Android + native build orchestration |
| **Target SDK** | Android 15 (API 35) | Latest platform features |
| **Min SDK** | Android 8.0 (API 26) | Broad device compatibility |
| **ABIs** | `arm64-v8a`, `x86_64` | Real devices + emulator |
| **Page Alignment** | 16 KB (`max-page-size=16384`) | Android 15 compatibility |
| **Target Hardware** | iQOO 15 (Snapdragon 8 Elite Gen 5) | Hackathon demo device |

---

## 📁 Project Structure

```
PocketMesh/
├── app/
│   ├── build.gradle.kts                  # App-level build config (CMake, NDK, deps)
│   ├── LlamaEngine.kt                   # ⚠️ Duplicate — older version (see note)
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml        # Permissions: INTERNET, WIFI, WAKE_LOCK
│           ├── cpp/
│           │   ├── CMakeLists.txt         # Native build: pocketmesh-native → llama
│           │   ├── PocketMesh-native.cpp  # JNI functions: load, generate, unload
│           │   └── llama.cpp/             # Git submodule → ggml-org/llama.cpp
│           └── java/com/example/pocketmesh/
│               ├── MainActivity.kt        # Compose UI: Node Monitor dashboard
│               ├── PocketMesh.kt          # NanoHTTPD server with route handlers
│               ├── ServerManager.kt       # Server lifecycle: start/stop, StateFlow
│               ├── LlamaEngine.kt         # JNI wrapper: sync + coroutine methods
│               ├── ChatService.kt         # Interface + MockChatService fallback
│               ├── LlamaChatService.kt    # Live inference via LlamaEngine
│               ├── ChatRequest.kt         # Data class: { message: String }
│               ├── StatusResponse.kt      # Data class: uptime, device, ip, requests
│               ├── NetworkUtils.kt        # Wi-Fi / Hotspot IP detection (RFC 1918)
│               └── ui/theme/              # Default Compose theme (unused)
│                   ├── Color.kt
│                   ├── Theme.kt
│                   └── Type.kt
├── build.gradle.kts                      # Root plugins: AGP, Kotlin, Compose
├── settings.gradle.kts                   # Project name + module includes
├── gradle.properties                     # JVM args, AndroidX config
└── gradle/
    └── libs.versions.toml                # Version catalog
```

> **⚠️ Note:** `app/LlamaEngine.kt` (23 lines) is a **duplicate/dead file** from an earlier iteration. The active version is at `app/src/main/java/.../LlamaEngine.kt` (104 lines) with full StateFlow, sync methods, and coroutine wrappers.

> **⚠️ Note:** The `ui/theme/` package uses `com.example.pocketmesh` while all other files use `com.pocketmesh.app`. The theme files are scaffolded defaults not actively used — MainActivity defines its own `PocketDarkColorScheme` inline.

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or later |
| Android SDK | API 35 (compileSdk) |
| NDK | Latest stable (with CMake 3.22.1) |
| JDK | 17 |
| Device/Emulator | ARM64 or x86_64, API 26+ |
| Free RAM | ≥ 1 GB available for model loading |

### Model Setup

PocketMesh requires a **GGUF-format model** on the device. The app auto-discovers the **first `.gguf` file** from:

1. `/storage/emulated/0/Android/data/com.pocketmesh.app/files/`
2. `/storage/emulated/0/Download/`

**Recommended model:**

```bash
# Qwen 0.5B Q4_K_M — ~400 MB, optimized for mobile
# Download from: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF
```

Transfer to device:

```bash
adb push Qwen2.5-0.5B-Instruct-Q4_K_M.gguf /storage/emulated/0/Download/
```

### Build Instructions

```bash
# 1. Clone the repository
git clone https://github.com/your-username/PocketMesh.git
cd PocketMesh

# 2. Initialize the llama.cpp submodule (REQUIRED)
git submodule update --init --recursive

# 3. Open in Android Studio → Sync Gradle → Build

# 4. Run on connected ARM64 device or x86_64 emulator
```

> **Important:** `llama.cpp` is a git submodule. You **must** run `git submodule update --init --recursive` or the native build will fail.

### Running on Android

1. Launch **PocketMesh**
2. Tap **"Start"** → HTTP server starts on port 8080
3. Tap **"Run Qwen"** → loads GGUF model into memory
4. Status shows **"Active & Ready"** → inference available
5. Test locally or remotely:

```bash
# From any device on the same Wi-Fi:
curl http://<PHONE_IP>:8080/health
curl -X POST http://<PHONE_IP>:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is edge computing?"}'
```

---

## 📡 Local API Documentation

Server binds to `0.0.0.0:8080`. Three endpoints available:

### `GET /` or `GET /health`

Health check.

```bash
curl http://192.168.1.100:8080/health
```

```json
{
  "status": "ok",
  "server": "PocketMesh",
  "source": "local"
}
```

### `GET /status`

Node telemetry and device information.

```bash
curl http://192.168.1.100:8080/status
```

```json
{
  "server": "PocketMesh",
  "status": "running",
  "uptime_ms": 45230,
  "ip": "192.168.1.100",
  "port": 8080,
  "device": "Pixel 8",
  "android_version": "15",
  "local_only": true,
  "total_requests": 12
}
```

### `POST /chat`

Send a prompt to the on-device LLM.

```bash
curl -X POST http://192.168.1.100:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Explain edge AI in one sentence."}'
```

**Success:**
```json
{
  "response": "Edge AI refers to running artificial intelligence models directly on local devices...",
  "model": "qwen-0.5b",
  "status": "success",
  "latency_ms": 3420
}
```

**Error (model not loaded):**
```json
{
  "response": "Error: Qwen model is not loaded. Load the model first.",
  "model": "qwen-0.5b",
  "status": "error",
  "latency_ms": 1
}
```

### CORS Headers

All responses include:
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

---

## 🔒 Security Model

### Current Prototype Security

> ⚠️ **PocketMesh is a prototype.** The current security model is designed for **local development and demonstration**, not production.

| Aspect | Current State | Risk |
|---|---|---|
| **Authentication** | ❌ None | Any LAN device can access all endpoints |
| **Authorization** | ❌ None | No per-route access control |
| **CORS** | `Allow-Origin: *` | Any origin can make requests |
| **TLS/HTTPS** | ❌ HTTP only | Unencrypted on local network |
| **Bind Address** | `0.0.0.0` | Reachable from all network interfaces |
| **Rate Limiting** | ❌ None | Susceptible to request flooding |
| **Input Validation** | Basic null/empty checks | No prompt size limits |

### Production Hardening Roadmap

- [ ] API key / token authentication
- [ ] TLS with self-signed certificates
- [ ] Bind to specific interfaces only
- [ ] Request rate limiting
- [ ] Prompt size limits and sanitization
- [ ] Device pairing with trust establishment

---

## ⚡ Performance Considerations

### Native Optimizations Present

- **C++17 with `-O3`** — maximum compiler optimization
- **2 CPU threads** — `n_threads = 2` to prevent thermal throttling
- **1024 context window** — conservative for mobile RAM
- **96 max generation tokens** — bounded output length
- **Greedy (argmax) sampling** — deterministic token selection; no temperature/top-p yet
- **KV cache clearing** — `llama_memory_seq_rm()` resets between requests
- **16 KB page alignment** — Android 15 kernel compatibility

### Benchmark Methodology

No benchmark numbers provided (device-dependent). Measure on your hardware:

| Metric | How to Measure |
|---|---|
| Model Load Time | Timestamp before/after `loadModelSync()` |
| Time to First Token | Start of `generateSync()` to first decode return |
| Tokens/sec | `generated_tokens / total_time` |
| Peak RAM | `ActivityManager.MemoryInfo` during inference |
| Battery Temp | `BatteryManager.EXTRA_TEMPERATURE` under load |
| Request Latency | `ChatResponse.latency_ms` field |

| Metric | Qwen 0.5B Q4_K_M | Your Device |
|---|---|---|
| Model Load Time | ___ ms | |
| Time to First Token | ___ ms | |
| Tokens/sec | ___ tok/s | |
| Peak RAM Usage | ___ MB | |
| Device Temperature | ___ °C | |
| Battery Impact (10 min) | ___ % | |

---

## 📊 Current Implementation Status

| Component | Status | Notes |
|---|---|---|
| Android UI (Node Monitor) | ✅ Implemented | Compose dashboard with 5 sections |
| GGUF Model Loading | ✅ Implemented | Auto-discovers `.gguf` from storage |
| llama.cpp Integration | ✅ Implemented | Git submodule, CMake, native linking |
| JNI Bridge (C++ ↔ Kotlin) | ✅ Implemented | 3 native methods: load, generate, unload |
| HTTP Server (NanoHTTPD) | ✅ Implemented | 3 endpoints + CORS + error handling |
| LLM Inference Pipeline | ✅ Implemented | ChatML, greedy sampling, EOG detection |
| Network IP Detection | ✅ Implemented | Wi-Fi, Hotspot, USB tethering |
| Hardware Diagnostics | ✅ Implemented | RAM, battery temp, request telemetry |
| Model Lifecycle | ✅ Implemented | Full state machine with StateFlow |
| Request Logging | ✅ Implemented | Timestamped with IP, route, latency |
| ChatService Abstraction | ✅ Implemented | Interface with Mock + Live implementations |
| Concurrent Inference Guard | 🟡 Partial | `@Synchronized` — sequential only, no queue |
| Streaming Responses | ❌ Not implemented | Full generation before response |
| Temperature/Top-p Sampling | ❌ Not implemented | Currently greedy/deterministic only |
| NPU Acceleration (ExecuTorch) | 🚧 Planned | Hackathon stretch goal |
| Peer Discovery | 🚧 Planned | UI has placeholder, no protocol |
| Device-to-Device Mesh | 🚧 Planned | Architecture anticipates, not implemented |
| Authentication | ❌ Not implemented | No auth on any endpoint |
| Rate Limiting | ❌ Not implemented | No request throttling |
| RAG / Documents | 🚧 Planned | Hackathon stretch goal — not in current codebase |
| Foreground Service | 🟡 Partial | Permission declared, service not built |
| Multi-model Support | ❌ Not implemented | Loads first `.gguf` found |

---

## ⚠️ Limitations

1. **Single-threaded inference** — `@Synchronized` means one inference at a time; concurrent requests queue
2. **No streaming** — full response generated before returning
3. **96 token output cap** — hardcoded in native layer
4. **1024 context window** — limited conversation memory
5. **Stateless requests** — no multi-turn conversation history
6. **No model selection** — loads first `.gguf` found, no UI for choosing
7. **Foreground only** — no background service despite permission being declared
8. **Package name mismatch** — `com.pocketmesh.app` vs `com.example.pocketmesh` in theme files
9. **Duplicate file** — orphaned `app/LlamaEngine.kt` alongside active version in `src/`
10. **Hardcoded threads** — `n_threads = 2` not configurable per device
11. **No model validation** — any `.gguf` attempted, even incompatible ones
12. **Duplicate permissions** — `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` declared twice in manifest
13. **Deterministic output only** — greedy/argmax sampling means no temperature or top-p control, so responses can be repetitive on longer generations

---

## 🔧 Engineering Challenges

### Mobile LLM Memory Pressure
GGUF models must fit in available RAM alongside the OS, apps, and UI. Qwen 0.5B (~400 MB quantized) is sized for this — larger models risk OOM on ≤ 4 GB devices.

### JNI/Native Integration Complexity
The Kotlin → JNI → C++ → llama.cpp chain crosses 4 runtime boundaries. String encoding (`GetStringUTFChars`/`ReleaseStringUTFChars`), null checks, and error propagation require careful handling. The implementation correctly manages JNI string lifecycle.

### Thermal Throttling
Sustained inference raises CPU temperature. PocketMesh uses `n_threads = 2` (not all cores) and displays battery temp in UI. No automatic throttling — the app doesn't slow down when temperature is high.

### Sequential Inference Bottleneck
`@Synchronized` ensures thread safety but creates queuing: 5 concurrent `/chat` requests execute one at a time. With 96-token generation, wait times compound.

### 16 KB Page Size Compatibility
Android 15 requires 16 KB page-aligned shared libraries. Addressed with `-Wl,-z,max-page-size=16384` in both CMake and Gradle configs.

### Model Lifecycle on Mobile
Android can kill background processes. The model (hundreds of MB) may need reloading after the app returns from background. `StateFlow<Boolean>` tracks state but there's no auto-reload.

### Local Network Variability
IP detection handles Wi-Fi, Hotspot, USB tethering, and OEM-specific interfaces (`wlan0`, `ap0`, `softap0`, `rndis0`). `NetworkUtils` handles this with interface prioritization and RFC 1918 filtering.

---

## 🗺️ Roadmap

### Phase 1: Stable Local AI Node *(current)*
> Reliable single-device inference with network API.
- [x] On-device GGUF inference via llama.cpp
- [x] Local HTTP API (health, status, chat)
- [x] Node monitor dashboard
- [ ] Fix duplicate files and package inconsistencies
- [ ] Configurable thread count and context size
- [ ] Temperature/top-p sampling (currently greedy-only)
- [ ] Streaming response support (SSE)
- [ ] Foreground service for background operation

### Phase 1.5: Hackathon Stretch Goals *(this week)*
> Features targeted specifically for the iQOO 15 demo.
- [ ] NPU-accelerated inference via ExecuTorch
- [ ] Offline RAG — document upload + on-device vector search
- [ ] Anti-tamper security layer using IMU + camera
- [ ] Office Kit-connected live telemetry dashboard

### Phase 2: Hardened Node
> Production-grade security and reliability.
- [ ] API token authentication
- [ ] TLS for local HTTPS
- [ ] Request rate limiting
- [ ] Prompt size limits and input sanitization
- [ ] Automatic thermal throttling
- [ ] Model selection UI (multiple GGUFs)

### Phase 3: Peer-to-Peer Discovery
> Devices on the same network find each other.
- [ ] mDNS / DNS-SD service advertisement
- [ ] Node identity (device ID + capabilities)
- [ ] Peer list with health monitoring
- [ ] Device pairing and trust model

### Phase 4: Distributed AI Runtime
> Route inference to the best available node.
- [ ] Capability advertisement (model, RAM, CPU, battery, temp)
- [ ] Task routing and load balancing
- [ ] Fallback and retry across nodes
- [ ] Consensus on model availability

### Phase 5: Local Knowledge Infrastructure
> Nodes host and share knowledge.
- [ ] Local RAG with file/document indexing
- [ ] Embedding generation on-device
- [ ] Knowledge sharing across mesh
- [ ] Local file serving via API

### Phase 6: Intelligent Mobile Infrastructure
> Self-organizing mesh adapting to resources.
- [ ] Model-aware scheduling
- [ ] Battery-aware task distribution
- [ ] Offline-first sync on reconnect
- [ ] Cross-platform support (iOS, desktop)

---

## 🔮 Future Distributed Architecture

> **⚠️ FUTURE CONCEPT — Not implemented.** The following describes a potential evolution of PocketMesh.

### Multi-Node Mesh Vision

```
                     PocketMesh Network (Future)

           ┌──────────────────┐
           │     Node A       │
           │  Qwen 0.5B       │
           │  RAM: 6 GB free  │
           │  Temp: 34°C      │
           │  Battery: 78%    │
           └────────┬─────────┘
                    │
            ┌───────┴───────┐
            │  Local Mesh   │  (mDNS / Wi-Fi Direct)
            │  Discovery    │
            └───┬───────┬───┘
                │       │
      ┌─────────▼──┐  ┌─▼───────────┐
      │   Node B   │  │   Node C    │
      │  Qwen 1.5B │  │  No model   │
      │  RAM: 3 GB │  │  RAM: 8 GB  │
      │  Temp: 41°C│  │  File server│
      │  Bat: 45%  │  │  Bat: 92%   │
      └────────────┘  └─────────────┘
```

### Future Intelligent Node Selection

```
    ┌─────────────────────────────────┐
    │     Incoming Inference Task     │
    └───────────────┬─────────────────┘
                    │
                    ▼
    ┌─────────────────────────────────┐
    │       Scheduler (Future)        │
    │                                 │
    │  Task Requirements              │
    │     + Model Availability        │
    │     + Available RAM             │
    │     + CPU Capability            │
    │     + Battery Level             │
    │     + Device Temperature        │
    │     + Network Quality           │
    │     + Current Load              │
    │              ↓                  │
    │     Select Optimal Node         │
    └───────────────┬─────────────────┘
                    │
         ┌──────────┼──────────┐
         ▼          ▼          ▼
      Node A     Node B     Node C
      (best)     (backup)   (offline)
```

---

## 💼 Use Cases

> PocketMesh is a **prototype**. These are realistic scenarios the architecture enables, not production deployments.

| Use Case | Description |
|---|---|
| **Offline AI Assistant** | LLM on your phone — airplane mode, underground, rural |
| **Campus Networks** | AI inference across university Wi-Fi without cloud costs |
| **Privacy-Sensitive Inference** | Medical, legal, personal prompts never leave device |
| **Field / Disaster Response** | AI without cell towers — just phones + hotspot |
| **Local Dev Server** | Mobile AI API for testing without cloud API keys |
| **Edge Computing Research** | Study on-device inference, thermal, and power behavior |
| **Temporary Infrastructure** | AI at events using just phones and a Wi-Fi hotspot |
| **Collaborative AI** *(future)* | Devices pool resources for multi-model inference |
| **Knowledge Hub** *(future)* | Phone serves docs + AI for a local team |

---

## 🧭 Design Principles

1. **Local-first** — the device is primary compute, not a cloud proxy
2. **Zero dependency** — no accounts, no API keys, no subscriptions required
3. **Infrastructure thinking** — treat a phone like a server, not just a client
4. **Honest engineering** — separate what works from what's planned
5. **Mobile-aware** — respect RAM, thermal, and battery constraints
6. **Incrementally distributed** — one node today, mesh tomorrow
7. **Open stack** — llama.cpp, GGUF, NanoHTTPD — no proprietary lock-in

---

## 🤝 Contributing

PocketMesh is in early development. Contributions welcome:

| Area | Examples |
|---|---|
| **Native/C++** | Streaming, multi-model, GGML optimizations, temperature sampling |
| **Kotlin** | Foreground service, multi-turn chat, state management |
| **Networking** | mDNS discovery, peer protocol, TLS |
| **UI** | Model browser, chat interface, better diagnostics |
| **Security** | Auth, rate limiting, input validation |
| **Docs** | Benchmarks, device-specific setup guides |
| **Testing** | Unit tests, integration tests, benchmarks |

```bash
git checkout -b feature/your-feature
# Make changes
git commit -m "feat: description"
git push origin feature/your-feature
# Open a Pull Request
```

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

## 🏷️ Keywords

`edge-ai` `on-device-ai` `local-llm` `android-llm` `llama-cpp` `gguf` `mobile-ai` `offline-ai` `edge-computing` `distributed-ai` `peer-to-peer` `ai-inference` `local-inference` `android` `kotlin` `cpp` `jni` `mesh-network` `mobile-infrastructure` `qwen` `jetpack-compose` `nanohttpd` `arm64` `on-device-inference` `privacy-ai` `iqoo` `snapdragon` `executorch`

---

<p align="center">
  <strong>PocketMesh</strong> — Your phone is not just a client. It's infrastructure.
</p>
