package com.ariy.retrolauncher

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val PFS0_ENTRY_SIZE = 0x18
private const val HFS0_ENTRY_SIZE = 0x40
private const val ROOT_XCI_HFS0_OFFSET = 0xF000L
private const val TICKET_RIGHTS_ID_OFFSET = 0x160L
private const val TICKET_RIGHTS_ID_SIZE = 0x10
private const val NCA_MAGIC_OFFSET = 0x200L
private const val NCA_TITLE_ID_OFFSET = 0x210L
private const val NCA_HEADER_READ_SIZE = 0x218
private const val CONTAINER_HEADER_SIZE = 0x10

private data class SwitchContainerEntry(
    val name: String,
    val offset: Long,
    val size: Long
)

fun inferSwitchTitleIdFromPackageUri(context: Context, uri: Uri): String? {
    return withSwitchReadableChannel(context, uri) { channel ->
        inferSwitchTitleIdFromChannel(channel, uri)
    }
}

private fun inferSwitchTitleIdFromChannel(
    channel: FileChannel,
    uri: Uri
): String? {
    inferTitleIdFromNcaHeader(channel, 0L)?.let { return it }
    parsePartitionFs(channel, 0L)?.let { entries ->
        inferTitleIdFromEntries(channel, 0L, entries)?.let { return it }
    }
    if (looksLikeXci(uri, channel.size())) {
        parsePartitionFs(channel, ROOT_XCI_HFS0_OFFSET)?.let { rootEntries ->
            inferTitleIdFromEntries(channel, ROOT_XCI_HFS0_OFFSET, rootEntries)?.let { return it }
        }
    }
    return null
}

private fun inferTitleIdFromEntries(
    channel: FileChannel,
    containerBaseOffset: Long,
    entries: List<SwitchContainerEntry>
): String? {
    entries.firstNotNullOfOrNull { entry ->
        inferTitleIdFromTicket(channel, containerBaseOffset + entry.offset, entry)
    }?.let { return it }

    entries.firstNotNullOfOrNull { entry ->
        inferTitleIdFromNcaEntry(channel, containerBaseOffset + entry.offset, entry)
    }?.let { return it }

    entries.filter { entry ->
        entry.name.equals("secure", ignoreCase = true) ||
            entry.name.equals("update", ignoreCase = true) ||
            entry.name.endsWith(".hfs0", ignoreCase = true)
    }.forEach { nested ->
        parsePartitionFs(channel, containerBaseOffset + nested.offset)?.let { nestedEntries ->
            inferTitleIdFromEntries(channel, containerBaseOffset + nested.offset, nestedEntries)?.let { return it }
        }
    }

    return null
}

private fun inferTitleIdFromTicket(
    channel: FileChannel,
    entryAbsoluteOffset: Long,
    entry: SwitchContainerEntry
): String? {
    if (!entry.name.endsWith(".tik", ignoreCase = true)) return null
    if (entry.size < TICKET_RIGHTS_ID_OFFSET + TICKET_RIGHTS_ID_SIZE) return null
    val rightsId = readBytes(
        channel = channel,
        offset = entryAbsoluteOffset + TICKET_RIGHTS_ID_OFFSET,
        size = TICKET_RIGHTS_ID_SIZE
    ) ?: return null
    return decodeTitleIdCandidates(rightsId.copyOfRange(0, 8)).firstOrNull(::looksLikeSwitchTitleId)
}

private fun inferTitleIdFromNcaEntry(
    channel: FileChannel,
    entryAbsoluteOffset: Long,
    entry: SwitchContainerEntry
): String? {
    if (!entry.name.endsWith(".nca", ignoreCase = true)) return null
    return inferTitleIdFromNcaHeader(channel, entryAbsoluteOffset)
}

private fun inferTitleIdFromNcaHeader(
    channel: FileChannel,
    absoluteOffset: Long
): String? {
    val header = readBytes(channel, absoluteOffset, NCA_HEADER_READ_SIZE) ?: return null
    val magic = header.copyOfRange(NCA_MAGIC_OFFSET.toInt(), NCA_MAGIC_OFFSET.toInt() + 4)
        .toString(Charsets.US_ASCII)
    if (magic != "NCA2" && magic != "NCA3") return null
    val titleIdBytes = header.copyOfRange(NCA_TITLE_ID_OFFSET.toInt(), NCA_TITLE_ID_OFFSET.toInt() + 8)
    return decodeTitleIdCandidates(titleIdBytes).firstOrNull(::looksLikeSwitchTitleId)
}

private fun parsePartitionFs(
    channel: FileChannel,
    baseOffset: Long
): List<SwitchContainerEntry>? {
    val header = readBytes(channel, baseOffset, CONTAINER_HEADER_SIZE) ?: return null
    val magic = header.copyOfRange(0, 4).toString(Charsets.US_ASCII)
    val byteBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.position(4)
    val fileCount = byteBuffer.int
    val stringTableSize = byteBuffer.int
    if (fileCount <= 0 || stringTableSize < 0) return null
    return when (magic) {
        "PFS0" -> parsePfs0Entries(channel, baseOffset, fileCount, stringTableSize)
        "HFS0" -> parseHfs0Entries(channel, baseOffset, fileCount, stringTableSize)
        else -> null
    }
}

private fun parsePfs0Entries(
    channel: FileChannel,
    baseOffset: Long,
    fileCount: Int,
    stringTableSize: Int
): List<SwitchContainerEntry>? {
    val entryTableSize = fileCount.toLong() * PFS0_ENTRY_SIZE
    val stringTableOffset = baseOffset + CONTAINER_HEADER_SIZE + entryTableSize
    val dataOffset = stringTableOffset + stringTableSize
    val stringTable = readBytes(channel, stringTableOffset, stringTableSize) ?: return null
    return buildList {
        repeat(fileCount) { index ->
            val entryOffset = baseOffset + CONTAINER_HEADER_SIZE + (index * PFS0_ENTRY_SIZE.toLong())
            val rawEntry = readBytes(channel, entryOffset, PFS0_ENTRY_SIZE) ?: return@repeat
            val entryBuffer = ByteBuffer.wrap(rawEntry).order(ByteOrder.LITTLE_ENDIAN)
            val offset = entryBuffer.long
            val size = entryBuffer.long
            val nameOffset = entryBuffer.int
            if (size <= 0L) return@repeat
            val name = readNullTerminatedAscii(stringTable, nameOffset) ?: return@repeat
            add(
                SwitchContainerEntry(
                    name = name,
                    offset = dataOffset + offset - baseOffset,
                    size = size
                )
            )
        }
    }.takeIf { it.isNotEmpty() }
}

private fun parseHfs0Entries(
    channel: FileChannel,
    baseOffset: Long,
    fileCount: Int,
    stringTableSize: Int
): List<SwitchContainerEntry>? {
    val entryTableSize = fileCount.toLong() * HFS0_ENTRY_SIZE
    val stringTableOffset = baseOffset + CONTAINER_HEADER_SIZE + entryTableSize
    val dataOffset = stringTableOffset + stringTableSize
    val stringTable = readBytes(channel, stringTableOffset, stringTableSize) ?: return null
    return buildList {
        repeat(fileCount) { index ->
            val entryOffset = baseOffset + CONTAINER_HEADER_SIZE + (index * HFS0_ENTRY_SIZE.toLong())
            val rawEntry = readBytes(channel, entryOffset, HFS0_ENTRY_SIZE) ?: return@repeat
            val entryBuffer = ByteBuffer.wrap(rawEntry).order(ByteOrder.LITTLE_ENDIAN)
            val offset = entryBuffer.long
            val size = entryBuffer.long
            val nameOffset = entryBuffer.int
            if (size <= 0L) return@repeat
            val name = readNullTerminatedAscii(stringTable, nameOffset) ?: return@repeat
            add(
                SwitchContainerEntry(
                    name = name,
                    offset = dataOffset + offset - baseOffset,
                    size = size
                )
            )
        }
    }.takeIf { it.isNotEmpty() }
}

private fun readNullTerminatedAscii(
    bytes: ByteArray,
    offset: Int
): String? {
    if (offset !in bytes.indices) return null
    var end = offset
    while (end < bytes.size && bytes[end].toInt() != 0) {
        end += 1
    }
    if (end <= offset) return null
    return bytes.copyOfRange(offset, end).toString(Charsets.US_ASCII)
}

private fun readBytes(
    channel: FileChannel,
    offset: Long,
    size: Int
): ByteArray? {
    if (size <= 0 || offset < 0) return null
    if (offset + size > channel.size()) return null
    val buffer = ByteBuffer.allocate(size)
    var readOffset = offset
    while (buffer.hasRemaining()) {
        val read = channel.read(buffer, readOffset)
        if (read <= 0) return null
        readOffset += read.toLong()
    }
    return buffer.array()
}

private fun decodeTitleIdCandidates(bytes: ByteArray): List<String> {
    if (bytes.size != 8) return emptyList()
    val direct = bytes.joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xFF) }
    val reversed = bytes.reversedArray().joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xFF) }
    return listOf(direct, reversed).distinct()
}

private fun looksLikeSwitchTitleId(value: String): Boolean =
    normalizeSwitchTitleId(value) != null

private fun looksLikeXci(
    uri: Uri,
    size: Long
): Boolean {
    val lower = uri.toString().lowercase()
    return lower.endsWith(".xci") || lower.endsWith(".xcz") || size > ROOT_XCI_HFS0_OFFSET + CONTAINER_HEADER_SIZE
}

private fun <T> withSwitchReadableChannel(
    context: Context,
    uri: Uri,
    block: (FileChannel) -> T?
): T? {
    if (uri.scheme == "file") {
        val path = uri.path?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            FileInputStream(File(path)).use { input ->
                block(input.channel)
            }
        }.getOrNull()
    }
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                block(input.channel)
            }
        }
    }.getOrNull()
}
