package com.mobclaw.android.provider.mlc

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Resolves MLC model directories using one of three strategies:
 *
 * 1. [MlcLoadSource.DOWNLOAD] — download from Hugging Face into external app storage
 * 2. [MlcLoadSource.EXTERNAL_STORAGE] — read from `Android/data/<package>/files/mlc_models/<modelId>/`
 * 3. [MlcLoadSource.ASSETS] — copy from `assets/mlc_models/<modelId>/` into external storage
 */
class MlcModelLoader private constructor(
    private val downloader: MlcHuggingFaceDownloader = MlcHuggingFaceDownloader(),
) {

    suspend fun resolveModelPath(
        context: Context,
        modelId: String,
        loadSource: MlcLoadSource,
        huggingFaceRepo: String? = null,
        onProgress: MlcLoadProgressListener? = null,
    ): String {
        val appContext = context.applicationContext
        val destDir = MlcPaths.externalModelDir(appContext, modelId)

        return when (loadSource) {
            MlcLoadSource.EXTERNAL_STORAGE -> resolveFromExternal(appContext, modelId, destDir, onProgress)
            MlcLoadSource.DOWNLOAD -> resolveFromDownload(
                appContext,
                modelId,
                destDir,
                huggingFaceRepo,
                onProgress,
            )
            MlcLoadSource.ASSETS -> resolveFromAssets(appContext, modelId, destDir, onProgress)
        }
    }

    private fun resolveFromExternal(
        context: Context,
        modelId: String,
        destDir: File,
        onProgress: MlcLoadProgressListener?,
    ): String {
        onProgress?.invoke(
            MlcLoadProgress(
                phase = MlcLoadProgress.Phase.CHECKING,
                modelId = modelId,
                message = "Looking for model in ${destDir.absolutePath}",
            )
        )

        if (MlcPaths.isValidModelDir(destDir)) {
            onProgress?.invoke(
                MlcLoadProgress(
                    phase = MlcLoadProgress.Phase.READY,
                    modelId = modelId,
                    message = "Using local model at ${destDir.absolutePath}",
                )
            )
            return destDir.absolutePath
        }

        throw IllegalStateException(
            "MLC model '$modelId' not found.\n" +
                "Copy weights to:\n${destDir.absolutePath}\n" +
                "Expected file: mlc-chat-config.json"
        )
    }

    private suspend fun resolveFromDownload(
        context: Context,
        modelId: String,
        destDir: File,
        huggingFaceRepo: String?,
        onProgress: MlcLoadProgressListener?,
    ): String {
        if (MlcPaths.isValidModelDir(destDir)) {
            onProgress?.invoke(
                MlcLoadProgress(
                    phase = MlcLoadProgress.Phase.READY,
                    modelId = modelId,
                    message = "Using cached download at ${destDir.absolutePath}",
                )
            )
            return destDir.absolutePath
        }

        val repo = huggingFaceRepo?.takeIf { it.isNotBlank() }
            ?: MlcAppConfigReader.read(context)?.findModel(modelId)?.defaultHuggingFaceRepo()
            ?: "mlc-ai/$modelId"

        downloader.downloadModel(
            repoId = repo,
            destDir = destDir,
            onProgress = onProgress,
            modelId = modelId,
        )
        return destDir.absolutePath
    }

    private fun resolveFromAssets(
        context: Context,
        modelId: String,
        destDir: File,
        onProgress: MlcLoadProgressListener?,
    ): String {
        if (MlcPaths.isValidModelDir(destDir)) {
            onProgress?.invoke(
                MlcLoadProgress(
                    phase = MlcLoadProgress.Phase.READY,
                    modelId = modelId,
                    message = "Using installed asset copy at ${destDir.absolutePath}",
                )
            )
            return destDir.absolutePath
        }

        val assetRoot = "mlc_models/$modelId"
        if (!hasAssetDir(context, assetRoot)) {
            throw IllegalStateException(
                "Bundled MLC model '$modelId' not found in assets/$assetRoot. " +
                    "Add weights under app/src/main/assets/$assetRoot or switch load source."
            )
        }

        onProgress?.invoke(
            MlcLoadProgress(
                phase = MlcLoadProgress.Phase.COPYING_ASSETS,
                modelId = modelId,
                message = "Installing bundled model to ${destDir.absolutePath}",
            )
        )

        copyAssetDir(context, assetRoot, destDir)

        if (!MlcPaths.isValidModelDir(destDir)) {
            throw IllegalStateException(
                "Failed to install bundled model from assets/$assetRoot to ${destDir.absolutePath}"
            )
        }

        onProgress?.invoke(
            MlcLoadProgress(
                phase = MlcLoadProgress.Phase.READY,
                modelId = modelId,
                message = "Bundled model installed at ${destDir.absolutePath}",
            )
        )
        return destDir.absolutePath
    }

    companion object {
        private val shared = MlcModelLoader()

        suspend fun resolveModelPath(
            context: Context,
            modelId: String,
            loadSource: MlcLoadSource,
            huggingFaceRepo: String? = null,
            onProgress: MlcLoadProgressListener? = null,
        ): String = shared.resolveModelPath(
            context = context,
            modelId = modelId,
            loadSource = loadSource,
            huggingFaceRepo = huggingFaceRepo,
            onProgress = onProgress,
        )

        fun externalModelPathHint(context: Context, modelId: String): String {
            return MlcPaths.displayPath(MlcPaths.externalModelDir(context, modelId))
        }

        private fun hasAssetDir(context: Context, assetPath: String): Boolean {
            return try {
                context.assets.list(assetPath)?.isNotEmpty() == true
            } catch (_: Exception) {
                false
            }
        }

        private fun copyAssetDir(context: Context, assetPath: String, destDir: File) {
            val children = context.assets.list(assetPath)
            if (children.isNullOrEmpty()) {
                copyAssetFile(context, assetPath, destDir)
                return
            }

            destDir.mkdirs()
            for (name in children) {
                val childAssetPath = "$assetPath/$name"
                val destFile = File(destDir, name)
                val nested = context.assets.list(childAssetPath)
                if (nested.isNullOrEmpty()) {
                    copyAssetFile(context, childAssetPath, destFile)
                } else {
                    copyAssetDir(context, childAssetPath, destFile)
                }
            }
        }

        private fun copyAssetFile(context: Context, assetPath: String, destFile: File) {
            destFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
