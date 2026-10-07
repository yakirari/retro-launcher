package net.rpcsx.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class FileUtilTest {
    @Test
    fun copyStreamExactly_preservesBinaryContentAcrossPartialFinalChunk() {
        val sourceBytes = ByteArray(1025) { index -> (index % 251).toByte() }
        val output = ByteArrayOutputStream()

        copyStreamExactly(sourceBytes.inputStream(), output)

        assertArrayEquals(sourceBytes, output.toByteArray())
    }
}
