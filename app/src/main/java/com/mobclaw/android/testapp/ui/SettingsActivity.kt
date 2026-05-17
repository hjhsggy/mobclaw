package com.mobclaw.android.testapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.mobclaw.android.provider.MlcProvider
import com.mobclaw.android.provider.mlc.MlcLoadSource
import com.mobclaw.android.testapp.config.MlcLoadSourceNames
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobclaw.android.testapp.config.ProviderConfig
import com.mobclaw.android.testapp.config.ProviderType
import com.mobclaw.android.testapp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(onBackPressed = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
    val configs by viewModel.configs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadAllConfigs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configure your LLM providers. Select one provider to enable it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ProviderType.entries.forEach { providerType ->
                        ProviderConfigCard(
                            providerType = providerType,
                            config = configs[providerType],
                            onSave = { config ->
                                scope.launch {
                                    viewModel.saveConfig(config)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    viewModel.deleteConfig(providerType)
                                }
                            },
                            onToggleEnabled = { enabled ->
                                scope.launch {
                                    viewModel.setEnabledProvider(providerType)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderConfigCard(
    providerType: ProviderType,
    config: ProviderConfig?,
    onSave: (ProviderConfig) -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    var apiKey by remember { mutableStateOf(config?.apiKey ?: "") }
    var model by remember { mutableStateOf(config?.model ?: providerType.getDefaultModel()) }
    var baseUrl by remember { mutableStateOf(config?.baseUrl ?: providerType.getDefaultBaseUrl()) }
    var mlcLoadSource by remember {
        mutableStateOf(
            config?.mlcLoadSource?.takeIf { it.isNotBlank() } ?: providerType.defaultMlcLoadSource()
        )
    }
    var isEditing by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isEnabled = config?.isEnabled == true
    val hasConfig = config != null
    val externalModelPath = if (providerType.isMlc()) {
        MlcProvider.externalModelPathHint(context, model.ifBlank { MlcProvider.DEFAULT_MODEL_ID })
    } else {
        ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEnabled) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isEnabled,
                        onCheckedChange = { onToggleEnabled(it) }
                    )
                    Text(
                        text = providerType.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasConfig) {
                        IconButton(
                            onClick = { isEditing = !isEditing }
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Filled.Save else Icons.Filled.Check,
                                contentDescription = if (isEditing) "Save" else "Edit",
                                tint = if (isEditing) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val newConfig = ProviderConfig(
                                    providerType = providerType,
                                    apiKey = apiKey,
                                    model = model,
                                    baseUrl = baseUrl,
                                    mlcLoadSource = mlcLoadSource,
                                    isEnabled = false
                                )
                                onSave(newConfig)
                            },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Configure")
                        }
                    }
                }
            }

            if (hasConfig && isEditing) {
                if (providerType.requiresApiKey()) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(providerType.getModelFieldLabel()) },
                    singleLine = true
                )

                if (providerType.isMlc()) {
                    Text(
                        text = "Load source",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MlcLoadSourceChip(
                            label = "Download",
                            selected = mlcLoadSource == MlcLoadSourceNames.DOWNLOAD,
                            onClick = { mlcLoadSource = MlcLoadSourceNames.DOWNLOAD }
                        )
                        MlcLoadSourceChip(
                            label = "SD Card",
                            selected = mlcLoadSource == MlcLoadSourceNames.EXTERNAL_STORAGE,
                            onClick = { mlcLoadSource = MlcLoadSourceNames.EXTERNAL_STORAGE }
                        )
                        MlcLoadSourceChip(
                            label = "Assets",
                            selected = mlcLoadSource == MlcLoadSourceNames.ASSETS,
                            onClick = { mlcLoadSource = MlcLoadSourceNames.ASSETS }
                        )
                    }

                    Text(
                        text = when (MlcLoadSource.fromConfig(mlcLoadSource)) {
                            MlcLoadSource.DOWNLOAD ->
                                "Downloads weights from Hugging Face into the app external directory."
                            MlcLoadSource.EXTERNAL_STORAGE ->
                                "Reads weights you placed under Android/data/<package>/files/mlc_models/<modelId>/."
                            MlcLoadSource.ASSETS ->
                                "Installs weights bundled at assets/mlc_models/<modelId>/ on first run."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Path:\n$externalModelPath",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (mlcLoadSource == MlcLoadSourceNames.DOWNLOAD) {
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(providerType.getBaseUrlFieldLabel()) },
                            placeholder = { Text("mlc-ai/gemma-3-1b-it-q4f16_1-MLC") },
                            singleLine = true
                        )
                    }
                } else if (baseUrl.isNotEmpty() || providerType.getDefaultBaseUrl().isNotEmpty()) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(providerType.getBaseUrlFieldLabel()) },
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        val updatedConfig = ProviderConfig(
                            providerType = providerType,
                            apiKey = apiKey,
                            model = model,
                            baseUrl = baseUrl,
                            mlcLoadSource = mlcLoadSource,
                            isEnabled = isEnabled
                        )
                        onSave(updatedConfig)
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Configuration")
                }
            } else if (hasConfig) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (providerType.requiresApiKey()) {
                        ConfigRow(
                            label = "API Key",
                            value = if (apiKey.isNotEmpty()) "••••••••••••" else "Not set"
                        )
                    }
                    ConfigRow(label = providerType.getModelFieldLabel(), value = model)
                    if (providerType.isMlc()) {
                        ConfigRow(
                            label = "Load source",
                            value = MlcLoadSource.fromConfig(mlcLoadSource).name
                        )
                        if (mlcLoadSource == MlcLoadSourceNames.DOWNLOAD) {
                            ConfigRow(
                                label = "HF Repo",
                                value = baseUrl.ifBlank { "mlc-ai/$model" }
                            )
                        }
                        Text(
                            text = externalModelPath,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (baseUrl.isNotEmpty()) {
                        ConfigRow(label = providerType.getBaseUrlFieldLabel(), value = baseUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun MlcLoadSourceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
fun ConfigRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}
