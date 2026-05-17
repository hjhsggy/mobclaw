package com.mobclaw.android.provider.mlc

/**
 * How MLC model weights are obtained before inference.
 */
enum class MlcLoadSource {
    /** Download from Hugging Face into the app external files directory. */
    DOWNLOAD,

    /** Read from `Android/data/<package>/files/mlc_models/<modelId>/`. */
    EXTERNAL_STORAGE,

    /** Copy bundled assets from `assets/mlc_models/<modelId>/` into external storage. */
    ASSETS;

    companion object {
        fun fromConfig(value: String?): MlcLoadSource {
            if (value.isNullOrBlank()) return EXTERNAL_STORAGE
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: EXTERNAL_STORAGE
        }
    }
}
