package com.mobclaw.android.provider

/**
 * Qwen provider via DashScope API.
 */
class QwenProvider(
    apiKey: String? = null,
    model: String = "qwen3-vl-plus-2025-12-19",
    baseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
) : OpenAiCompatibleProvider(
    apiKey = apiKey ?: "",
    model = model,
    baseUrl = baseUrl,
)
