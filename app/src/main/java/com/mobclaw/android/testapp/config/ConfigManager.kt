package com.mobclaw.android.testapp.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveProviderConfig(config: ProviderConfig) = withContext(Dispatchers.IO) {
        val key = getPrefsKey(config.providerType)
        val configJson = json.encodeToString(config)
        prefs.edit().putString(key, configJson).apply()
    }

    suspend fun loadProviderConfig(providerType: ProviderType): ProviderConfig? = withContext(Dispatchers.IO) {
        val key = getPrefsKey(providerType)
        val configJson = prefs.getString(key, null) ?: return@withContext null
        try {
            json.decodeFromString<ProviderConfig>(configJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadAllConfigs(): Map<ProviderType, ProviderConfig> = withContext(Dispatchers.IO) {
        ProviderType.entries.mapNotNull { providerType ->
            loadProviderConfig(providerType)?.let { config ->
                providerType to config
            }
        }.toMap()
    }

    suspend fun deleteProviderConfig(providerType: ProviderType) = withContext(Dispatchers.IO) {
        val key = getPrefsKey(providerType)
        prefs.edit().remove(key).apply()
    }

    suspend fun getEnabledProvider(): ProviderConfig? = withContext(Dispatchers.IO) {
        val allConfigs = loadAllConfigs()
        allConfigs.values.firstOrNull { it.isEnabled }
    }

    suspend fun setEnabledProvider(providerType: ProviderType) = withContext(Dispatchers.IO) {
        val allConfigs = loadAllConfigs()
        allConfigs.values.forEach { config ->
            val updatedConfig = config.copy(isEnabled = config.providerType == providerType)
            saveProviderConfig(updatedConfig)
        }
    }

    suspend fun clearAllConfigs() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private fun getPrefsKey(providerType: ProviderType): String {
        return "provider_config_${providerType.name.lowercase()}"
    }

    companion object {
        private const val PREFS_NAME = "mobclaw_config"
    }
}
