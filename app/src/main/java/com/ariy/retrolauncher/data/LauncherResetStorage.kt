package com.ariy.retrolauncher.data

import android.content.Context
import android.content.Intent
import java.io.File
import java.nio.file.Files

/** Only launcher-private outputs are deletion candidates. Never follow links into shared inputs. */
internal fun deletePrivateResetTree(file: File) {
    if (Files.isSymbolicLink(file.toPath())) {
        check(file.delete()) { "Could not remove private cache link" }
        return
    }
    if (!file.exists()) return
    if (file.isDirectory) {
        val children = checkNotNull(file.listFiles()) { "Could not list private cache" }
        children.forEach(::deletePrivateResetTree)
    }
    check(file.delete()) { "Could not remove private ${file.name}" }
}

internal class LauncherResetStorage(private val context: Context) {
    /** Best effort, with every failure reported rather than claiming a complete reset. */
    fun clear(): List<String> = buildList {
        fun attempt(label: String, action: () -> Unit) {
            try { action() } catch (error: Exception) {
                add("$label: ${error.message ?: error.javaClass.simpleName}")
            }
        }
        // This list is scoped by Android to grants held by THIS app/UID, not other apps.
        attempt("Reading saved permissions") {
            context.contentResolver.persistedUriPermissions.forEach { grant ->
                attempt("Releasing saved folder/artwork access") {
                    val flags = (if (grant.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                        (if (grant.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
                    context.contentResolver.releasePersistableUriPermission(grant.uri, flags)
                }
            }
        }
        attempt("Removing imported/downloaded artwork") {
            deletePrivateResetTree(File(context.filesDir, "shortcut-icons"))
        }
        // Includes temporary artwork downloads and extracted launch copies; never the source ROM.
        attempt("Removing private cache") { deletePrivateResetTree(context.cacheDir) }
        // All current callers use retro_launcher; also clear the Android default store if present.
        listOf("retro_launcher", "${context.packageName}_preferences").forEach { name ->
            attempt("Clearing $name settings") {
                check(context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
                    .putBoolean("setup_completed", false).commit()) { "Preferences could not be saved" }
            }
        }
    }
}