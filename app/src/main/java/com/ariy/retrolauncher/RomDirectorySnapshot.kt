package com.ariy.retrolauncher

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

internal data class RomDocument(val name: String, val uri: Uri, val isDirectory: Boolean)

/** Read child names and types in one SAF query instead of querying each DocumentFile repeatedly. */
internal fun listRomDocuments(
    context: Context,
    directory: DocumentFile,
    cancellationCheck: () -> Unit = {}
): List<RomDocument> {
    cancellationCheck()
    val parentId = DocumentsContract.getDocumentId(directory.uri)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(directory.uri, parentId)
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    )
    return withRomQueryCancellation(cancellationCheck) { signal ->
    val cursor = context.contentResolver.query(childrenUri, projection, null, null, null, signal)
        ?: error("The storage provider could not list the ROM folder")
    cursor.use {
        buildList {
            while (it.moveToNext()) {
                cancellationCheck()
                val id = it.getString(0) ?: continue
                val name = it.getString(1)?.takeIf(String::isNotBlank)
                    ?: id.substringAfterLast('/').substringAfterLast(':')
                add(RomDocument(
                    name = name,
                    uri = DocumentsContract.buildDocumentUriUsingTree(directory.uri, id),
                    isDirectory = it.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                ))
            }
        }
    }
    }
}