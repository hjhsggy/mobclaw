package com.mobclaw.android.testapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.mobclaw.android.testapp.ui.SettingsActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mobclaw.android.accessibility.MobClawAccessibilityService
import com.mobclaw.android.core.AgentResult
import com.mobclaw.android.core.MobAgent
import com.mobclaw.android.core.MobClawConfig
import com.mobclaw.android.overlay.AgentOverlay
import com.mobclaw.android.overlay.OverlayObserver
import com.mobclaw.android.provider.LlmProvider
import com.mobclaw.android.provider.AnthropicProvider
import com.mobclaw.android.provider.GeminiProvider
import com.mobclaw.android.provider.OllamaProvider
import com.mobclaw.android.provider.OpenAiProvider
import com.mobclaw.android.provider.OpenRouterProvider
import com.mobclaw.android.provider.MlcProvider
import com.mobclaw.android.provider.QwenProvider
import com.mobclaw.android.provider.mlc.MlcLoadSource
import com.mobclaw.android.testapp.config.MlcDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mobclaw.android.testapp.config.ConfigManager
import com.mobclaw.android.testapp.config.ProviderConfig
import com.mobclaw.android.testapp.config.ProviderType
import kotlinx.coroutines.launch

/**
 * Simple test activity to exercise MobClaw agent.
 *
 * Usage:
 * 1. Open Settings to configure LLM providers
 * 2. Select and enable a provider
 * 3. Grant overlay permission
 * 4. Enable MobClaw accessibility service
 * 5. Type a task and hit "Execute"
 */
class MainActivity : ComponentActivity() {

    private val configManager = ConfigManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DemoScreen()
                }
            }
        }
    }

    @Composable
    private fun DemoScreen() {
        var task by remember { mutableStateOf("") }
        var isRunning by remember { mutableStateOf(false) }
        var resultText by remember { mutableStateOf("Results will appear here...") }
        var enabledProviderConfig by remember { mutableStateOf<ProviderConfig?>(null) }

        val mobMock = remember { com.mobmock.MobMock(this@MainActivity) }
        var loginSession by remember { mutableStateOf<com.mobmock.MobMock.LoginSession?>(null) }

        var accessibilityOk by remember { mutableStateOf(isAccessibilityEnabled()) }
        var overlayOk by remember { mutableStateOf(isOverlayPermissionGranted()) }
        val lifecycleOwner = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            enabledProviderConfig = configManager.getEnabledProvider()
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    accessibilityOk = isAccessibilityEnabled()
                    overlayOk = isOverlayPermissionGranted()
                    scope.launch {
                        enabledProviderConfig = configManager.getEnabledProvider()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🦀 MobClaw Test", style = MaterialTheme.typography.headlineMedium)

                if (!accessibilityOk || !overlayOk) {
                    Text(
                        text = buildString {
                            if (!accessibilityOk) appendLine("❌ Accessibility Service: tap button to enable")
                            if (!overlayOk) append("❌ Overlay Permission: tap button to grant")
                        }.trim(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (enabledProviderConfig == null) {
                    Text(
                        text = "⚠️ No provider configured. Open Settings to configure and enable a provider.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚙️ Provider Settings")
                }

                if (!accessibilityOk) {
                    Button(
                        onClick = { openAccessibilitySettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Accessibility Settings")
                    }
                }

                if (!overlayOk) {
                    Button(
                        onClick = { openOverlaySettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Overlay Permission")
                    }
                }

                if (enabledProviderConfig != null) {
                    Text(
                        text = "Enabled Provider: ${enabledProviderConfig!!.providerType.label}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedTextField(
                    value = task,
                    onValueChange = { task = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    label = { Text("Task (e.g. Open Settings and turn on Wi-Fi)") }
                )

                Button(
                    onClick = {
                        val config = enabledProviderConfig
                        val trimmedTask = task.trim()

                        if (config == null) {
                            Toast.makeText(
                                this@MainActivity,
                                "Please configure and enable a provider in Settings first",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (!config.isValid()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Provider configuration is invalid. Please check Settings.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (trimmedTask.isEmpty()) {
                            Toast.makeText(this@MainActivity, "Enter a task", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!isAccessibilityEnabled()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Enable MobClaw accessibility service first",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        if (!isOverlayPermissionGranted()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Grant overlay permission first",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        scope.launch {
                            if (config.providerType == ProviderType.MOBMOCK && !mobMock.isLoggedIn()) {
                                loginSession = mobMock.startLogin()
                                return@launch
                            }

                            isRunning = true
                            resultText = if (config.providerType == ProviderType.MLC) {
                                "🦀 Loading MLC model..."
                            } else {
                                "🦀 Executing..."
                            }
                            try {
                                val result = executeTask(
                                    mobMock = mobMock,
                                    config = config,
                                    task = trimmedTask,
                                    onStatus = { status ->
                                        scope.launch {
                                            withContext(Dispatchers.Main) {
                                                resultText = status
                                            }
                                        }
                                    },
                                )
                                val status = if (result.success) "✅ Success" else "❌ Failed"
                                resultText = buildString {
                                    appendLine("$status (${result.iterations} iterations, ${result.duration.inWholeSeconds}s)")
                                    appendLine()
                                    append(result.message)
                                }
                            } catch (e: Exception) {
                                resultText = "❌ Error: ${e.message}"
                            } finally {
                                isRunning = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRunning && enabledProviderConfig != null,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(if (isRunning) "Executing..." else "🚀 Execute Task")
                }

                Text(
                    text = resultText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    private suspend fun executeTask(
        mobMock: com.mobmock.MobMock,
        config: ProviderConfig,
        task: String,
        onStatus: (String) -> Unit = {},
    ): AgentResult {
        val agentOverlay = AgentOverlay(applicationContext)

        val provider = buildProvider(mobMock, config, onStatus)
        val agent = MobAgent.builder()
            .provider(provider)
            .observer(OverlayObserver(agentOverlay))
            .config(MobClawConfig())
            .build()

        agentOverlay.onStopRequested = {
            agent.cancel()
            agentOverlay.updateStatus("⏹ Stopping...")
        }

        return agent.execute(task)
    }

    private fun buildProvider(
        mobMock: com.mobmock.MobMock,
        config: ProviderConfig,
        onStatus: (String) -> Unit = {},
    ): LlmProvider {
        return when (config.providerType) {
            ProviderType.GEMINI -> GeminiProvider(apiKey = config.apiKey)
            ProviderType.OPENAI -> OpenAiProvider(apiKey = config.apiKey)
            ProviderType.ANTHROPIC -> AnthropicProvider(apiKey = config.apiKey)
            ProviderType.OPENROUTER -> OpenRouterProvider(apiKey = config.apiKey)
            ProviderType.QWEN -> QwenProvider(
                apiKey = config.apiKey,
                model = config.model,
                baseUrl = config.baseUrl
            )
            ProviderType.OLLAMA -> OllamaProvider(
                apiKey = config.apiKey,
                model = config.model,
                baseUrl = config.baseUrl
            )
            ProviderType.MOBMOCK -> com.mobclaw.android.testapp.provider.MobMockProvider(mobMock = mobMock)
            ProviderType.MLC -> MlcProvider(
                context = applicationContext,
                modelId = config.model.ifBlank { MlcDefaults.MODEL_ID },
                modelLib = MlcDefaults.MODEL_LIB,
                loadSource = MlcLoadSource.fromConfig(config.mlcLoadSource),
                huggingFaceRepo = config.baseUrl.takeIf { it.isNotBlank() },
                onLoadProgress = { progress ->
                    val label = progress.message
                        ?: progress.currentFile?.let { "Loading model: $it (${progress.completedFiles}/${progress.totalFiles})" }
                        ?: "Loading model (${progress.phase})..."
                    onStatus("🦀 $label")
                },
            )
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        return MobClawAccessibilityService.instance != null
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
        }
    }
}
