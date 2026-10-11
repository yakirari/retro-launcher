package com.ariy.retrolauncher.launch

import android.content.Context
import android.content.pm.LauncherApps
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.ariy.retrolauncher.listRomDocuments

/** Use the document URI's selected child, not fromTreeUri (which silently reads the tree root). */
internal fun scummVmFolder(context: Context, uri: Uri): DocumentFile? {
    val document = if (DocumentsContract.isTreeUri(uri)) {
        runCatching {
            DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
        }.getOrElse {
            DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        }
    } else uri
    return DocumentFile.fromSingleUri(context, document)?.takeIf { it.isDirectory }
}

internal fun detectScummVmFolder(context: Context, uri: Uri, cancellationCheck: () -> Unit = {}): String? {
    val root = scummVmFolder(context, uri) ?: return null
    val files = listRomDocuments(context, root, cancellationCheck).map { doc ->
        cancellationCheck()
        val name = doc.name
        val marker = !doc.isDirectory && (name.endsWith(".scummvm", true) || name.endsWith(".svm", true))
        val content = if (marker) runCatching {
            context.contentResolver.openInputStream(doc.uri)?.use { input ->
                String(input.readNBytes(1024), Charsets.UTF_8)
            }
        }.getOrNull() else null
        name to content
    }
    return ScummVmGames.detect(root.name.orEmpty(), files)
}

internal fun publishedScummVmTargets(context: Context): List<ScummVmGames.RegisteredGame> {
    val apps = context.getSystemService(LauncherApps::class.java)
    if (!apps.hasShortcutHostPermission()) return emptyList()
    val query = LauncherApps.ShortcutQuery().apply {
        setPackage(ScummVmGames.PACKAGE)
        setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED)
    }
    return runCatching { apps.getShortcuts(query, android.os.Process.myUserHandle()) }.getOrNull().orEmpty()
        .filter { it.isEnabled }.mapNotNull { shortcut ->
            val uri = shortcut.intents?.lastOrNull()?.data ?: return@mapNotNull null
            if (uri.scheme != "scummvm" || !ScummVmGames.validTarget(uri.schemeSpecificPart)) return@mapNotNull null
            ScummVmGames.RegisteredGame(uri.schemeSpecificPart, null, shortcut.shortLabel?.toString().orEmpty(), "")
        }
}