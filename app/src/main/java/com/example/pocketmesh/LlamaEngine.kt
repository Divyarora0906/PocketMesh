package com.pocketmesh.app
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

object LlamaEngine {
    private const val TAG = "PocketMesh-Llama"

    private var isLibraryLoaded = false

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        try {
            System.loadLibrary("pocketmesh-native")
            isLibraryLoaded = true
            Log.d(TAG, "Native library 'pocketmesh-native' loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}", e)
        }
    }

    private external fun loadModelNative(modelPath: String): Boolean
    private external fun generateResponseNative(prompt: String): String
    private external fun unloadModelNative()

    // ------------------ SYNCHRONOUS METHODS (Safe for NanoHTTPD / ChatService worker threads) ------------------

    @Synchronized
    fun loadModelSync(path: String): Boolean {
        if (!isLibraryLoaded) {
            Log.e(TAG, "Cannot load model: Native library is not loaded.")
            return false
        }

        val file = File(path)
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "Model file missing or unreadable at path: $path")
            return false
        }

        return try {
            Log.d(TAG, "Loading native GGUF model from: $path")
            val success = loadModelNative(path)
            _isLoaded.value = success
            if (success) {
                Log.d(TAG, "GGUF model loaded into memory successfully.")
            } else {
                Log.e(TAG, "Native loadModelNative returned false.")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error during native model load: ${e.message}", e)
            _isLoaded.value = false
            false
        }
    }

    @Synchronized
    fun generateSync(prompt: String): String {
        if (!isLibraryLoaded) return "Error: Native library not loaded"
        if (!_isLoaded.value) return "Error: Qwen model is not loaded. Load the model first."

        return try {
            generateResponseNative(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error during native generation: ${e.message}", e)
            "Error during inference: ${e.localizedMessage}"
        }
    }

    @Synchronized
    fun unloadSync() {
        if (isLibraryLoaded && _isLoaded.value) {
            try {
                unloadModelNative()
                Log.d(TAG, "Native model unloaded.")
            } catch (e: Exception) {
                Log.e(TAG, "Error unloading model: ${e.message}", e)
            } finally {
                _isLoaded.value = false
            }
        }
    }

    // ------------------ COROUTINE SUSPEND WRAPPERS (For Jetpack Compose UI / Coroutines) ------------------

    suspend fun loadModel(path: String): Boolean = withContext(Dispatchers.IO) {
        loadModelSync(path)
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        generateSync(prompt)
    }

    suspend fun unload() = withContext(Dispatchers.IO) {
        unloadSync()
    }
}