package com.mobclaw.android.provider.mlc

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MlcAppConfig(
    @SerialName("model_list") val modelList: List<MlcModelEntry> = emptyList(),
) {
    fun findModel(modelId: String): MlcModelEntry? {
        return modelList.firstOrNull { it.modelId == modelId }
    }
}

@Serializable
data class MlcModelEntry(
    @SerialName("model_id") val modelId: String,
    @SerialName("model_lib") val modelLib: String,
    @SerialName("model_url") val modelUrl: String? = null,
    @SerialName("estimated_vram_bytes") val estimatedVramBytes: Long? = null,
) {
    fun defaultHuggingFaceRepo(): String = "mlc-ai/$modelId"
}

object MlcAppConfigReader {
    private val json = Json { ignoreUnknownKeys = true }

    fun read(context: Context): MlcAppConfig? {
        return try {
            context.assets.open("mlc-app-config.json").use { stream ->
                json.decodeFromString<MlcAppConfig>(stream.bufferedReader().readText())
            }
        } catch (_: Exception) {
            null
        }
    }
}
