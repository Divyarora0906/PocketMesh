# PocketMesh Android App 📱🤖

PocketMesh is an Android application featuring local LLM integration using a native C++ engine (`llama.cpp`) and real-time network telemetry monitoring.

---

## 🛠️ Prerequisites & Setup

### 1. Clone `llama.cpp` into the C++ Layer
To compile the native library for local AI inference, you need the core source files from `llama.cpp`. 

Navigate to your project's C++ directory (`/app/src/main/cpp/`) and clone the official repository:

```bash
cd app/src/main/cpp/
git clone [https://github.com/ggerganov/llama.cpp.git](https://github.com/ggerganov/llama.cpp.git)
