package com.mobclaw.android.testapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobclaw.android.testapp.config.ConfigManager
import com.mobclaw.android.testapp.config.ProviderConfig
import com.mobclaw.android.testapp.config.ProviderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val configManager = ConfigManager(application)

    private val _configs = MutableStateFlow<Map<ProviderType, ProviderConfig>>(emptyMap())
    val configs: StateFlow<Map<ProviderType, ProviderConfig>> = _configs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAllConfigs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedConfigs = configManager.loadAllConfigs()
                _configs.value = loadedConfigs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveConfig(config: ProviderConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                configManager.saveProviderConfig(config)
                loadAllConfigs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteConfig(providerType: ProviderType) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                configManager.deleteProviderConfig(providerType)
                loadAllConfigs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setEnabledProvider(providerType: ProviderType) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                configManager.setEnabledProvider(providerType)
                loadAllConfigs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
