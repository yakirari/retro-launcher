package com.ariy.retrolauncher.data

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class LauncherResetStorageTest {
    @Test fun removesOnlySelectedPrivateOutputsAndKeepsRomAndSharedArtwork() {
        val root = Files.createTempDirectory("launcher-reset-test").toFile()
        try {
            val shared = root.resolve("shared").apply { mkdirs() }
            val rom = shared.resolve("game.rom").apply { writeText("ROM") }
            val cover = shared.resolve("cover.png").apply { writeText("original") }
            val privateArt = root.resolve("files/shortcut-icons").apply { mkdirs() }
            privateArt.resolve("download.png").writeText("private copy")
            val cache = root.resolve("cache/nested").apply { mkdirs() }
            cache.resolve("temporary.download").writeText("cache")
            deletePrivateResetTree(privateArt)
            deletePrivateResetTree(root.resolve("cache"))
            assertFalse(privateArt.exists())
            assertFalse(root.resolve("cache").exists())
            assertEquals("ROM", rom.readText())
            assertEquals("original", cover.readText())
            deletePrivateResetTree(privateArt) // Idempotent retry.
        } finally { root.deleteRecursively() }
    }

    @Test fun privateSymlinkNeverDeletesSharedTarget() {
        val root = Files.createTempDirectory("launcher-reset-links").toFile()
        try {
            val shared = root.resolve("shared").apply { mkdirs() }
            val rom = shared.resolve("game.rom").apply { writeText("ROM") }
            val cache = root.resolve("cache").apply { mkdirs() }
            Files.createSymbolicLink(cache.resolve("linked-input").toPath(), shared.toPath())
            deletePrivateResetTree(cache)
            assertFalse(cache.exists())
            assertEquals("ROM", rom.readText())
        } finally { root.deleteRecursively() }
    }
}