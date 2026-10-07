package net.rpcsx.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.database.Cursor
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.rpcsx.GameInfo
import net.rpcsx.GameRepository
import net.rpcsx.PrecompilerService
import net.rpcsx.PrecompilerServiceAction
import net.rpcsx.ProgressRepository
import net.rpcsx.R
import net.rpcsx.RPCSX
import net.rpcsx.provider.AppDataDocumentProvider
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.concurrent.thread

private data class InstallableFolder(
    val uri: Uri, val targetPath: String
)

private val exactManagedRpcsxLibraryName = Regex(
    pattern = """^librpcsx-android_[A-Za-z0-9.\-]+_[A-Za-z0-9.\-]+\.so$""",
    option = RegexOption.IGNORE_CASE
)
private val upstreamRpcsxLibraryAssetName = Regex(
    pattern = """^librpcsx-android-(?:[A-Za-z0-9.\-]+)-([A-Za-z0-9.\-]+)\.so$""",
    option = RegexOption.IGNORE_CASE
)
private val fileTokenSanitizer = Regex("[^A-Za-z0-9.\\-]+")
private val knownRpcsxArchNames = listOf(
    "armv9.1-a",
    "armv9-a",
    "armv8.5-a",
    "armv8.4-a",
    "armv8.2-a",
    "armv8.1-a",
    "armv8-a",
    "x86-64"
)

internal fun copyStreamExactly(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read == -1) break
        output.write(buffer, 0, read)
    }
}

object FileUtil {
    fun installPackages(context: Context, rootFolderUri: Uri) {
        thread {
            val workList = mutableListOf<Uri>()
            workList.add(rootFolderUri)

            val batchFiles = mutableListOf<Uri>()
            val batchDirs = mutableListOf<InstallableFolder>()

            while (workList.isNotEmpty()) {
                val currentFolderUri = workList.removeAt(0)

                val paramSfo =
                    uriOpenFile(context, currentFolderUri, "PS3_GAME/PARAM.SFO") ?: uriOpenFile(
                        context, currentFolderUri, "PARAM.SFO"
                    )

                if (paramSfo != null) {
                    val installDir =
                        RPCSX.instance.getDirInstallPath(paramSfo.parcelFileDescriptor.fd)
                    paramSfo.close()

                    if (installDir != null) {
                        batchDirs += InstallableFolder(currentFolderUri, installDir)
                    } else {
                        workList.add(currentFolderUri)
                    }

                    continue
                }

                listFiles(currentFolderUri, context).forEach { item ->
                    if (item.isDirectory) {
                        workList.add(item.uri)
                    } else {
                        batchFiles += item.uri
                    }
                }
            }

            if (batchFiles.isNotEmpty()) {
                PrecompilerService.start(
                    context, PrecompilerServiceAction.Install, ArrayList(batchFiles)
                )
            }

            batchDirs.forEach {
                if (GameRepository.find(it.targetPath) != null) {
                    return@forEach
                }

                val progress = ProgressRepository.create(context, context.getString(R.string.installing_dir))
                GameRepository.add(arrayOf(GameInfo("$")), progress)
                copyDirUriToInternalStorage(context, it.uri, it.targetPath, progress)
                RPCSX.instance.collectGameInfo(it.targetPath, -1L)
            }
        }
    }

    fun saveGameFolderUri(prefs: SharedPreferences, uri: Uri) {
        prefs.edit { putString("selected_game_folder", uri.toString()) }
    }

    fun copyDirUriToInternalStorage(
        context: Context, rootFolderUri: Uri, path: String, progressId: Long
    ) {
        val workList = mutableListOf<Pair<Uri, String>>()
        workList.add(Pair(rootFolderUri, path))
        val fileList = mutableListOf<Pair<Uri, String>>()

        while (workList.isNotEmpty()) {
            val currentFolderUriTarget = workList.removeAt(0)
            val currentFolderUri = currentFolderUriTarget.first
            val currentFolderTarget = currentFolderUriTarget.second

            listFiles(currentFolderUri, context).forEach { item ->
                val file = File(currentFolderTarget, item.filename)
                if (item.isDirectory) {
                    file.mkdirs()
                    workList.add(Pair(item.uri, file.path))
                } else {
                    fileList.add(Pair(item.uri, file.path))
                }
            }
        }

        ProgressRepository.onProgressEvent(progressId, 0, fileList.size.toLong())
        var processed = 0L

        fileList.forEach { file ->
            saveFile(context, file.first, file.second)
            ProgressRepository.onProgressEvent(progressId, ++processed, fileList.size.toLong())
        }
    }

    fun saveFile(context: Context, source: Uri, target: String): Boolean {
        val targetFile = File(target)
        targetFile.parentFile?.mkdirs()

        return try {
            val input = context.contentResolver.openInputStream(source) ?: return false
            input.use { sourceStream ->
                BufferedInputStream(sourceStream).use { bufferedInput ->
                    BufferedOutputStream(FileOutputStream(targetFile, false)).use { bufferedOutput ->
                        copyStreamExactly(bufferedInput, bufferedOutput)
                        bufferedOutput.flush()
                    }
                }
            }
            true
        } catch (e: IOException) {
            Log.e("FileUtil", "Failed to save file to $target", e)
            false
        }
    }

    fun resolveImportedRpcsxLibraryTarget(context: Context, source: Uri): File {
        val sourceName = DocumentFile.fromSingleUri(context, source)
            ?.name
            ?.trim()
            .orEmpty()

        val managedName = when {
            sourceName.matches(exactManagedRpcsxLibraryName) -> sourceName
            else -> {
                val inferredArch = upstreamRpcsxLibraryAssetName.find(sourceName)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: knownRpcsxArchNames.firstOrNull { sourceName.contains(it, ignoreCase = true) }
                    ?: "unknown"
                "librpcsx-android_${sanitizeFileToken(inferredArch)}_manual.so"
            }
        }

        return File(context.filesDir.canonicalFile, managedName)
    }

    fun uriChild(context: Context, rootUri: Uri, path: String): SimpleDocument? {
        val pathDirectories = path.split("/").toMutableList()
        var uri = rootUri
        val filename = pathDirectories.removeAt(pathDirectories.size - 1)

        while (pathDirectories.isNotEmpty()) {
            val dirName = pathDirectories.removeAt(0)
            val entry = listFiles(uri, context).find { it.filename == dirName }
            if (entry == null || !entry.isDirectory) {
                return null
            }

            uri = entry.uri
        }

        return listFiles(uri, context).find { it.filename == filename }
    }

    fun uriOpenFile(context: Context, rootUri: Uri, path: String): AssetFileDescriptor? {
        val entry = uriChild(context, rootUri, path)

        if (entry == null || entry.isDirectory) {
            return null
        }

        return context.contentResolver.openAssetFileDescriptor(entry.uri, "r")
    }

    fun listFiles(uri: Uri, context: Context): Array<SimpleDocument> {
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        var c: Cursor? = null
        val results: MutableList<SimpleDocument> = ArrayList()
        try {
            val docId = if (isRootTreeUri(uri)) {
                DocumentsContract.getTreeDocumentId(uri)
            } else {
                DocumentsContract.getDocumentId(uri)
            }

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
            c = context.contentResolver.query(childrenUri, columns, null, null, null)
            while (c!!.moveToNext()) {
                val documentId = c.getString(0)
                val documentName = c.getString(1)
                val documentMimeType = c.getString(2)
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                val document = SimpleDocument(documentName, documentMimeType, documentUri)
                results.add(document)
            }
        } catch (e: Exception) {
            Log.e("FileUtil", "Cannot list file error: " + e.message)
        } finally {
            c?.close()
        }
        return results.toTypedArray<SimpleDocument>()
    }

    fun isRootTreeUri(uri: Uri): Boolean {
        val paths = uri.pathSegments
        return paths.size == 2 && "tree" == paths[0]
    }

    fun deleteCache(ctx: Context, gameId: String, onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = File(ctx.getExternalFilesDir(null)!!, "cache/cache/$gameId").deleteRecursively()
            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }

    fun importConfig(ctx: Context, uri: Uri): Boolean {
        return try {
            val docFile = DocumentFile.fromSingleUri(ctx, uri)
            if (docFile == null || (docFile.name?.endsWith(".yml", true) != true)) return false
            val inputStream: InputStream = ctx.contentResolver.openInputStream(uri) ?: return false
            val outputFile: File = ctx.getExternalFilesDir(null)?.resolve("config")?.resolve("config.yml") ?: return false
            val outputStream: OutputStream = outputFile.outputStream()
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportConfig(ctx: Context, uri: Uri): Boolean {
        return try {
            val inputFile = ctx.getExternalFilesDir(null)?.resolve("config")?.resolve("config.yml") ?: return false
            val inputStream: InputStream = inputFile.inputStream()
            val outputStream: OutputStream = ctx.contentResolver.openOutputStream(uri) ?: return false
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
           true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun launchInternalDir(ctx: Context): Boolean {
        if (!ctx.launchBrowseIntent(Intent.ACTION_VIEW)) {
            if (!ctx.launchBrowseIntent()) {
                if (!ctx.launchBrowseIntent(Intent.ACTION_OPEN_DOCUMENT_TREE)) {
                    return false
                }
            }
        }
        return true
    }

    private fun Context.launchBrowseIntent(
        action: String = "android.provider.action.BROWSE"
    ): Boolean {
        return try {
            val intent = Intent(action).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = DocumentsContract.buildRootUri(
                    AppDataDocumentProvider.AUTHORITY, AppDataDocumentProvider.ROOT_ID
                )
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            println("No activity found to handle $action intent")
            false
        }
    }
}

private fun sanitizeFileToken(value: String): String =
    value.trim()
        .lowercase()
        .replace(fileTokenSanitizer, "-")
        .trim('-', '.')
        .ifBlank { "unknown" }

class SimpleDocument(val filename: String, val mimeType: String, val uri: Uri) {
    val isDirectory: Boolean
        get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
}
