package com.mobclaw.android.provider.mlc

data class MlcLoadProgress(
    val phase: Phase,
    val modelId: String,
    val completedFiles: Int = 0,
    val totalFiles: Int = 0,
    val currentFile: String? = null,
    val message: String? = null,
) {
    enum class Phase {
        CHECKING,
        DOWNLOADING,
        COPYING_ASSETS,
        READY,
        FAILED,
    }

    val fraction: Float
        get() = if (totalFiles <= 0) 0f else completedFiles.toFloat() / totalFiles.toFloat()
}

typealias MlcLoadProgressListener = (MlcLoadProgress) -> Unit
