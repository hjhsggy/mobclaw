package com.mobclaw.android.provider.mlc

import ai.mlc.mlcllm.MLCEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe singleton holder for [MLCEngine].
 * Model reload and inference are serialized on a single engine instance.
 */
internal object MlcEngineManager {

    private val mutex = Mutex()
    private var engine: MLCEngine? = null
    private var loadedModelPath: String? = null
    private var loadedModelLib: String? = null

    suspend fun reload(modelPath: String, modelLib: String) {
        mutex.withLock {
            val eng = engine ?: MLCEngine().also { engine = it }
            if (loadedModelPath != modelPath || loadedModelLib != modelLib) {
                eng.reload(modelPath, modelLib)
                loadedModelPath = modelPath
                loadedModelLib = modelLib
            }
        }
    }

    suspend fun <T> withEngine(block: suspend (MLCEngine) -> T): T {
        return mutex.withLock {
            val eng = engine ?: MLCEngine().also { engine = it }
            block(eng)
        }
    }
}
