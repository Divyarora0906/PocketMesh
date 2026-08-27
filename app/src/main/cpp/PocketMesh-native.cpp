#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <cstdlib>
#include <ctime>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "PocketMeshNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static int g_n_ctx = 1024; // Shared context limit

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pocketmesh_app_LlamaEngine_loadModelNative(JNIEnv* env, jobject thiz, jstring modelPath) {
    if (modelPath == nullptr) {
        LOGE("Model path is null");
        return JNI_FALSE;
    }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) {
        LOGE("Failed to get UTF characters for model path");
        return JNI_FALSE;
    }

    // Clean up existing context/model if re-loading
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    g_model = llama_model_load_from_file(path, model_params);

    env->ReleaseStringUTFChars(modelPath, path);

    if (!g_model) {
        LOGE("Failed to load GGUF model from path");
        return JNI_FALSE;
    }

    // Optimized context & thread parameters to prevent mobile thermal throttling
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = g_n_ctx;
    ctx_params.n_threads = 2;       // Use 2 threads to prevent CPU core contention
    ctx_params.n_threads_batch = 2; // Use 2 threads for batch processing

    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("Failed to initialize context from model");
        llama_model_free(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    LOGI("Model loaded successfully into context with 2 threads & %d context", g_n_ctx);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_pocketmesh_app_LlamaEngine_generateResponseNative(JNIEnv* env, jobject thiz, jstring promptStr) {
    if (!g_ctx || !g_model) {
        LOGE("Attempted generation on uninitialized model/context");
        return env->NewStringUTF("Error: Model not initialized");
    }

    if (promptStr == nullptr) {
        return env->NewStringUTF("Error: Null prompt provided");
    }

    // CRITICAL FIX: Clear sequence 0 KV cache using the modern memory-layout API
    llama_memory_seq_rm(llama_get_memory(g_ctx), 0, -1, -1);

    const char* rawPrompt = env->GetStringUTFChars(promptStr, nullptr);
    if (!rawPrompt) {
        return env->NewStringUTF("Error: Failed to read prompt");
    }

    // Wrap the raw user message in Qwen's ChatML chat template to keep it grounded
    std::string formattedPrompt =
            "<|im_start|>system\nYou are PocketMesh, a helpful assistant.<|im_end|>\n"
            "<|im_start|>user\n" + std::string(rawPrompt) + "<|im_end|>\n"
                                                            "<|im_start|>assistant\n";

    env->ReleaseStringUTFChars(promptStr, rawPrompt);

    const char* prompt = formattedPrompt.c_str();

    // 1. Tokenize the formatted prompt using the vocab pointer
    const auto* vocab = llama_model_get_vocab(g_model);
    int n_vocab = llama_n_vocab(vocab);

    std::vector<llama_token> tokens(formattedPrompt.size() + 128);

    int n_tokens = llama_tokenize(vocab, prompt, (int)formattedPrompt.size(), tokens.data(), (int)tokens.size(), true, true);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt, (int)formattedPrompt.size(), tokens.data(), (int)tokens.size(), true, true);
    }
    tokens.resize(n_tokens);

    // Guard against context overflow
    if ((int)tokens.size() >= g_n_ctx) {
        LOGE("Prompt too long for context window");
        return env->NewStringUTF("Error: Prompt too long for context window");
    }

    // Initialize batch and evaluate prompt tokens
    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); i++) {
        batch.token[i] = tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == tokens.size() - 1);
    }
    batch.n_tokens = tokens.size();

    if (llama_decode(g_ctx, batch) != 0) {
        LOGE("llama_decode failed on prompt");
        llama_batch_free(batch);
        return env->NewStringUTF("Error: Native decode failed");
    }

    // 2. Generation Loop with Temperature Sampling to prevent premature cutoff
    std::string generated_text = "";
    const std::string stopMarker = "<|im_end|>";
    srand(time(nullptr));

    for (int i = 0; i < 96; i++) { // Up to 96 tokens for complete answers
        if ((int)(tokens.size() + i) >= g_n_ctx) {
            break;
        }

        auto* logits = llama_get_logits_ith(g_ctx, batch.n_tokens - 1);
        int n_vocab_val = llama_n_vocab(vocab);

        // Temperature sampling configuration
        float temperature = 0.7f;
        float max_logit = -1e30f;
        for (int v = 0; v < n_vocab_val; v++) {
            if (logits[v] > max_logit) {
                max_logit = logits[v];
            }
        }

        std::vector<float> probabilities(n_vocab_val);
        float sum_exp = 0.0f;
        for (int v = 0; v < n_vocab_val; v++) {
            probabilities[v] = expf((logits[v] - max_logit) / temperature);
            sum_exp += probabilities[v];
        }

        float r = static_cast<float>(rand()) / static_cast<float>(RAND_MAX);
        float cumulative = 0.0f;
        llama_token new_token_id = 0;

        for (int v = 0; v < n_vocab_val; v++) {
            probabilities[v] /= sum_exp;
            cumulative += probabilities[v];
            if (r <= cumulative) {
                new_token_id = v;
                break;
            }
        }

        // Check for End-Of-Generation token
        if (llama_vocab_is_eog(vocab, new_token_id)) {
            break;
        }

        // Convert token to piece and append
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            generated_text.append(buf, n);
        }

        // Safety fallback check for text stop markers
        size_t stopPos = generated_text.find(stopMarker);
        if (stopPos != std::string::npos) {
            generated_text.resize(stopPos);
            break;
        }

        // Prepare batch for the single newly generated token
        llama_batch_free(batch);
        batch = llama_batch_init(1, 0, 1);
        batch.token[0] = new_token_id;
        batch.pos[0] = tokens.size() + i;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;
        batch.n_tokens = 1;

        if (llama_decode(g_ctx, batch) != 0) {
            LOGE("llama_decode failed during generation step");
            break;
        }
    }

    llama_batch_free(batch);

    return env->NewStringUTF(generated_text.empty() ? "(Empty generation)" : generated_text.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_pocketmesh_app_LlamaEngine_unloadModelNative(JNIEnv* env, jobject thiz) {
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    llama_backend_free();
    LOGI("Model unloaded successfully");
}