package com.mobclaw.android.provider.mlc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

internal class MlcHuggingFaceDownloader(
    private val client: OkHttpClient = defaultClient(),
) {

    suspend fun downloadModel(
        repoId: String,
        destDir: File,
        onProgress: MlcLoadProgressListener?,
        modelId: String,
    ): File = withContext(Dispatchers.IO) {
        val normalizedRepo = repoId.trim().removePrefix("HF://").removeSuffix("/")
        require(normalizedRepo.contains("/")) {
            "Hugging Face repo must look like 'mlc-ai/model-name', got: $repoId"
        }

        destDir.mkdirs()
        onProgress?.invoke(
            MlcLoadProgress(
                phase = MlcLoadProgress.Phase.CHECKING,
                modelId = modelId,
                message = "Fetching file list from huggingface.co/$normalizedRepo",
            )
        )

        val remoteFiles = fetchRemoteFilePaths(normalizedRepo)
        val filesToDownload = remoteFiles.ifEmpty { fallbackFilesForRepo(normalizedRepo) }

        filesToDownload.forEachIndexed { index, relativePath ->
            onProgress?.invoke(
                MlcLoadProgress(
                    phase = MlcLoadProgress.Phase.DOWNLOADING,
                    modelId = modelId,
                    completedFiles = index,
                    totalFiles = filesToDownload.size,
                    currentFile = relativePath,
                    message = "Downloading $relativePath",
                )
            )

            val destFile = File(destDir, relativePath)
            destFile.parentFile?.mkdirs()
            downloadFile(normalizedRepo, relativePath, destFile)
        }

        if (!MlcPaths.isValidModelDir(destDir)) {
            throw IllegalStateException(
                "Download finished but mlc-chat-config.json is missing in ${destDir.absolutePath}"
            )
        }

        onProgress?.invoke(
            MlcLoadProgress(
                phase = MlcLoadProgress.Phase.READY,
                modelId = modelId,
                completedFiles = filesToDownload.size,
                totalFiles = filesToDownload.size,
                message = "Model ready at ${destDir.absolutePath}",
            )
        )
        destDir
    }

    private fun fetchRemoteFilePaths(repoId: String): List<String> {
        val request = Request.Builder()
            .url("https://huggingface.co/api/models/$repoId/tree/main?recursive=true")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val nodes = json.decodeFromString<List<HfTreeNode>>(body)
            return nodes
                .filter { it.type == "file" }
                .map { it.path }
                .filter { path ->
                    !path.startsWith(".") &&
                        !path.endsWith(".md", ignoreCase = true) &&
                        !path.endsWith(".gitattributes", ignoreCase = true)
                }
        }
    }

    private fun downloadFile(repoId: String, relativePath: String, destFile: File) {
        if (destFile.exists() && destFile.length() > 0L) {
            return
        }

        val url = "https://huggingface.co/$repoId/resolve/main/$relativePath"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Failed to download $relativePath (${response.code}): ${response.message}"
                )
            }
            val body = response.body ?: throw IllegalStateException("Empty body for $relativePath")
            destFile.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
    }

    private fun fallbackFilesForRepo(repoId: String): List<String> {
        // Default Gemma 3 1B MLC bundle layout.
        if (repoId.endsWith("gemma-3-1b-it-q4f16_1-MLC")) {
            return listOf(
                "mlc-chat-config.json",
                "ndarray-cache.json",
                "tensor-cache.json",
                "tokenizer.json",
                "tokenizer.model",
                "tokenizer_config.json",
                "added_tokens.json",
            ) + (0..14).map { "params_shard_$it.bin" }
        }
        throw IllegalStateException(
            "Could not list files for $repoId. Open the model on Hugging Face or add a local copy under " +
                "Android/data/<package>/files/mlc_models/."
        )
    }

    @Serializable
    private data class HfTreeNode(
        val type: String,
        val path: String,
        @SerialName("size") val size: Long? = null,
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
