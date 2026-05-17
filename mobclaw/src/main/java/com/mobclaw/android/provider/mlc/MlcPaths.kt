package com.mobclaw.android.provider.mlc

import android.content.Context
import java.io.File

object MlcPaths {

    /**
     * App-specific external directory: `/sdcard/Android/data/<package>/files/`.
     */
    fun externalFilesRoot(context: Context): File {
        return context.getExternalFilesDir(null)
            ?: throw IllegalStateException("External files directory is unavailable on this device")
    }

    /** `{externalFiles}/mlc_models/{modelId}/` */
    fun externalModelDir(context: Context, modelId: String): File {
        return File(externalFilesRoot(context), "mlc_models/$modelId")
    }

    fun isValidModelDir(dir: File): Boolean {
        return dir.isDirectory && File(dir, "mlc-chat-config.json").exists()
    }

    fun displayPath(dir: File): String = dir.absolutePath
}
