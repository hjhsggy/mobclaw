package com.mobclaw.android.provider

import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionStreamResponse
import ai.mlc.mlcllm.OpenAIProtocol.StreamOptions
import android.content.Context
import com.mobclaw.android.model.ChatMessage
import com.mobclaw.android.model.ChatResponse
import com.mobclaw.android.model.ToolSpec
import com.mobclaw.android.provider.mlc.MlcAppConfigReader
import com.mobclaw.android.provider.mlc.MlcEngineManager
import com.mobclaw.android.provider.mlc.MlcLoadProgressListener
import com.mobclaw.android.provider.mlc.MlcLoadSource
import com.mobclaw.android.provider.mlc.MlcMessageMapper
import com.mobclaw.android.provider.mlc.MlcModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device LLM provider backed by [ai.mlc.mlcllm.MLCEngine].
 *
 * Model weights can be loaded via [loadSource]:
 * - [MlcLoadSource.DOWNLOAD]: Hugging Face download to app external storage
 * - [MlcLoadSource.EXTERNAL_STORAGE]: `Android/data/<package>/files/mlc_models/<modelId>/`
 * - [MlcLoadSource.ASSETS]: bundled under `assets/mlc_models/<modelId>/`
 */
class MlcProvider(
    private val context: Context,
    private val modelId: String = DEFAULT_MODEL_ID,
    private val modelLib: String = DEFAULT_MODEL_LIB,
    private val loadSource: MlcLoadSource = MlcLoadSource.EXTERNAL_STORAGE,
    private val huggingFaceRepo: String? = null,
    private val onLoadProgress: MlcLoadProgressListener? = null,
    private val maxTokens: Int = 1024,
) : LlmProvider {

    @Volatile
    private var resolvedModelPath: String? = null

    override suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>?,
        model: String?,
        temperature: Double,
    ): ChatResponse = withContext(Dispatchers.IO) {
        ensureModelLoaded()
        MlcEngineManager.withEngine { engine ->
            val channel = engine.chat.completions.create(
                messages = MlcMessageMapper.toMlcMessages(messages),
                model = model ?: modelId,
                max_tokens = maxTokens,
                temperature = temperature.toFloat(),
                stream = true,
                stream_options = StreamOptions(include_usage = true),
            )

            val text = collectStreamText(channel)
            ChatResponse(text = text.ifBlank { null })
        }
    }

    override fun supportsNativeTools(): Boolean = false

    private suspend fun ensureModelLoaded(): String {
        val cached = resolvedModelPath
        if (cached != null) return cached

        val effectiveLib = resolveModelLib()
        val path = MlcModelLoader.resolveModelPath(
            context = context.applicationContext,
            modelId = modelId,
            loadSource = loadSource,
            huggingFaceRepo = huggingFaceRepo,
            onProgress = onLoadProgress,
        )
        MlcEngineManager.reload(path, effectiveLib)
        resolvedModelPath = path
        return path
    }

    private fun resolveModelLib(): String {
        if (modelLib != DEFAULT_MODEL_LIB) return modelLib
        return MlcAppConfigReader.read(context.applicationContext)
            ?.findModel(modelId)
            ?.modelLib
            ?: modelLib
    }

    private suspend fun collectStreamText(
        channel: kotlinx.coroutines.channels.ReceiveChannel<ChatCompletionStreamResponse>,
    ): String {
        val builder = StringBuilder()
        for (chunk in channel) {
            chunk.choices.forEach { choice ->
                choice.delta.content?.asText()?.let { builder.append(it) }
            }
        }
        return builder.toString()
    }

    companion object {
        const val DEFAULT_MODEL_ID = "gemma-3-1b-it-q4f16_1-MLC"
        const val DEFAULT_MODEL_LIB = "gemma3_text_q4f16_1_cb71ea40e2e72cd6e29dcd10d368db59"

        fun externalModelPathHint(context: Context, modelId: String = DEFAULT_MODEL_ID): String {
            return MlcModelLoader.externalModelPathHint(context, modelId)
        }
    }
}
