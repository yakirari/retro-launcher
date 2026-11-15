package com.ariy.retrolauncher.model

import com.ariy.retrolauncher.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PlatformSvgIconsTest {
    @Test
    fun xboxPlatformsUseTheXboxBadgeAsset() {
        assertEquals(R.raw.platform_xbox, PlatformSvgIcons.rawResourceForPlatform("Microsoft Xbox"))
        assertEquals(R.raw.platform_xbox, PlatformSvgIcons.rawResourceForPlatform("xbox"))
    }
}
