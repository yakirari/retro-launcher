package com.ariy.retrolauncher.launch

object EdenPackages {
    const val STABLE = "dev.eden.eden_emulator"
    const val NIGHTLY = "dev.eden.eden_emulator.nightly"
    const val OPTIMIZED = "com.miHoYo.Yuanshen"
    const val CUSTOM_CONFIG_ACTION = "$STABLE.LAUNCH_WITH_CUSTOM_CONFIG"

    val supported = setOf(STABLE, NIGHTLY, OPTIMIZED)

    fun isEdenPackage(packageName: String?): Boolean = packageName != null && packageName in supported

    fun customConfigAction(packageName: String?): String? =
        CUSTOM_CONFIG_ACTION.takeIf { isEdenPackage(packageName) }
}
