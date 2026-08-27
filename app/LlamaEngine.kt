package com.pocketmesh.app

import android.util.Log

object LlamaEngine {

    init {
        try {
            System.loadLibrary("pocketmesh-native")
            Log.d("PocketMesh", "pocketmesh-native library loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("PocketMesh", "Failed to load pocketmesh-native library: ${e.message}")
        }
    }

    private external fun loadModelNative(modelPath: String): Boolean
    private external fun generateResponseNative(prompt: String): String
    private external fun unloadModelNative()

    fun loadModel(path: String): Boolean = loadModelNative(path)
    fun generate(prompt: String): String = generateResponseNative(prompt)
    fun unload() = unloadModelNative()
}