package com.ariy.retrolauncher

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwitchTitleIdDetectorTest {
    @Test fun readsActualRightsIdAfterRsa2048Signature() {
        assertEquals("0100ABCD12345000", detectTicket(0x010004, "0100ABCD12345000"))
        assertEquals("0100ABCD12345800", detectTicket(0x010004, "0100ABCD12345800"))
    }

    @Test fun supportsOtherAlignedSignatures() {
        assertEquals("0100ABCD12345000", detectTicket(0x010003, "0100ABCD12345000"))
        assertEquals("0100ABCD12345000", detectTicket(0x010005, "0100ABCD12345000"))
    }

    @Test fun unknownSignatureDoesNotReadArbitraryBytesAsTitleId() {
        assertNull(detectTicket(0, "0100ABCD12345000"))
    }

    private fun detectTicket(signatureType: Int, titleId: String): String? {
        val ticket = ByteArray(0x500)
        ByteBuffer.wrap(ticket).order(ByteOrder.BIG_ENDIAN).putInt(signatureType)
        val rightsOffset = switchTicketRightsIdOffset(signatureType)?.toInt() ?: 0x160
        titleId.chunked(2).forEachIndexed { index, hex -> ticket[rightsOffset + index] = hex.toInt(16).toByte() }
        val name = "content.tik\u0000".toByteArray()
        val archive = ByteBuffer.allocate(0x10 + 0x18 + name.size + ticket.size).order(ByteOrder.LITTLE_ENDIAN)
        archive.put("PFS0".toByteArray()).putInt(1).putInt(name.size).putInt(0)
        archive.putLong(0).putLong(ticket.size.toLong()).putInt(0).putInt(0)
        archive.put(name).put(ticket)
        val file = File.createTempFile("switch-ticket", ".nsp")
        return try {
            file.writeBytes(archive.array())
            file.inputStream().use { inferSwitchTitleIdFromChannel(it.channel) }
        } finally {
            file.delete()
        }
    }
}