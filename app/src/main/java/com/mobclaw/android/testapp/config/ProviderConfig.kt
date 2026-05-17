package com.mobclaw.android.testapp.config

import kotlinx.serialization.Serializable

@Serializable
data class ProviderConfig(
    val providerType: ProviderType,
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = "",
    /** MLC only: DOWNLOAD | EXTERNAL_STORAGE | ASSETS */
    val mlcLoadSource: String = "",
    val isEnabled: Boolean = false,
) {
    fun isValid(): Boolean {
        return when (providerType) {
            ProviderType.GEMINI -> apiKey.isNotEmpty()
            ProviderType.OPENAI -> apiKey.isNotEmpty()
            ProviderType.ANTHROPIC -> apiKey.isNotEmpty()
            ProviderType.OPENROUTER -> apiKey.isNotEmpty()
            ProviderType.QWEN -> apiKey.isNotEmpty()
            ProviderType.OLLAMA -> true
            ProviderType.MOBMOCK -> true
            ProviderType.MLC -> true
        }
    }
}

@Serializable
enum class ProviderType(val label: String) {
    GEMINI("Gemini"),
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    OPENROUTER("OpenRouter"),
    QWEN("Qwen"),
    OLLAMA("Ollama"),
    MOBMOCK("MobMock (ChatGPT)"),
    MLC("MLC (On-Device)");

    fun getDefaultModel(): String {
        return when (this) {
            GEMINI -> "gemini-2.0-flash-exp"
            OPENAI -> "gpt-4o"
            ANTHROPIC -> "claude-3-5-sonnet-20241022"
            OPENROUTER -> "anthropic/claude-3.5-sonnet"
            QWEN -> "qwen3-vl-plus-2025-12-19"
            OLLAMA -> "llama3.2"
            MOBMOCK -> ""
            MLC -> MlcDefaults.MODEL_ID
        }
    }

    fun getDefaultBaseUrl(): String {
        return when (this) {
            GEMINI -> ""
            OPENAI -> ""
            ANTHROPIC -> ""
            OPENROUTER -> ""
            QWEN -> "https://dashscope.aliyuncs.com/compatible-mode/v1"
            OLLAMA -> "http://127.0.0.1:11434/v1"
            MOBMOCK -> ""
            MLC -> ""
        }
    }

    fun requiresApiKey(): Boolean {
        return when (this) {
            GEMINI, OPENAI, ANTHROPIC, OPENROUTER, QWEN -> true
            OLLAMA, MOBMOCK, MLC -> false
        }
    }

    fun isMlc(): Boolean = this == MLC

    fun getModelFieldLabel(): String = when (this) {
        MLC -> "Model ID"
        else -> "Model"
    }

    fun getBaseUrlFieldLabel(): String = when (this) {
        MLC -> "Hugging Face Repo (download only)"
        else -> "Base URL"
    }

    fun defaultMlcLoadSource(): String = MlcLoadSourceNames.EXTERNAL_STORAGE
}

object MlcLoadSourceNames {
    const val DOWNLOAD = "DOWNLOAD"
    const val EXTERNAL_STORAGE = "EXTERNAL_STORAGE"
    const val ASSETS = "ASSETS"
}

/** Defaults mirrored from `dist/lib/mlc4j/src/main/assets/mlc-app-config.json`. */
object MlcDefaults {
    const val MODEL_ID = com.mobclaw.android.provider.MlcProvider.DEFAULT_MODEL_ID
    const val MODEL_LIB = com.mobclaw.android.provider.MlcProvider.DEFAULT_MODEL_LIB
}
