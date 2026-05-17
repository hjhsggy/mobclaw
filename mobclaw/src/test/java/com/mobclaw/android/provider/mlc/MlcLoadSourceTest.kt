package com.mobclaw.android.provider.mlc

import org.junit.Assert.assertEquals
import org.junit.Test

class MlcLoadSourceTest {

    @Test
    fun fromConfig_defaultsToExternalStorage() {
        assertEquals(MlcLoadSource.EXTERNAL_STORAGE, MlcLoadSource.fromConfig(null))
        assertEquals(MlcLoadSource.EXTERNAL_STORAGE, MlcLoadSource.fromConfig(""))
        assertEquals(MlcLoadSource.EXTERNAL_STORAGE, MlcLoadSource.fromConfig("unknown"))
    }

    @Test
    fun fromConfig_parsesKnownValues() {
        assertEquals(MlcLoadSource.DOWNLOAD, MlcLoadSource.fromConfig("DOWNLOAD"))
        assertEquals(MlcLoadSource.ASSETS, MlcLoadSource.fromConfig("assets"))
        assertEquals(MlcLoadSource.EXTERNAL_STORAGE, MlcLoadSource.fromConfig("external_storage"))
    }
}
