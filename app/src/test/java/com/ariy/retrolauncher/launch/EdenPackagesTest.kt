package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GamePlatforms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdenPackagesTest {
    @Test
    fun supportsStableAndNightlyPackageNames() {
        assertEquals("dev.eden.eden_emulator.nightly", EdenPackages.NIGHTLY)
        assertTrue(EdenPackages.isEdenPackage(EdenPackages.STABLE))
        assertTrue(EdenPackages.isEdenPackage(EdenPackages.NIGHTLY))
        assertTrue(EdenPackages.isEdenPackage(EdenPackages.OPTIMIZED))
        assertFalse(EdenPackages.isEdenPackage("dev.eden.unknown"))
        assertFalse(EdenPackages.isEdenPackage(null))
    }

    @Test
    fun buildsCustomConfigActionForEachPackage() {
        assertEquals(
            "dev.eden.eden_emulator.LAUNCH_WITH_CUSTOM_CONFIG",
            EdenPackages.customConfigAction(EdenPackages.STABLE)
        )
        assertEquals(
            "dev.eden.eden_emulator.LAUNCH_WITH_CUSTOM_CONFIG",
            EdenPackages.customConfigAction(EdenPackages.NIGHTLY)
        )
        assertEquals(
            "dev.eden.eden_emulator.LAUNCH_WITH_CUSTOM_CONFIG",
            EdenPackages.customConfigAction(EdenPackages.OPTIMIZED)
        )
        assertEquals(null, EdenPackages.customConfigAction("dev.eden.unknown"))
    }

    @Test
    fun infersSwitchPlatformForNightlyPackage() {
        assertEquals("Nintendo Switch", GamePlatforms.inferFromPackageName(EdenPackages.NIGHTLY))
        assertEquals("Nintendo Switch", GamePlatforms.inferFromPackageName(EdenPackages.OPTIMIZED))
    }
}
