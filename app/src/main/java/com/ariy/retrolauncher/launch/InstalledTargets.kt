package com.ariy.retrolauncher.launch

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Bundle
import android.util.DisplayMetrics
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import org.json.JSONObject

data class InstalledShortcutOption(
    val title: String,
    val subtitle: String,
    val sourceLabel: String,
    val packageName: String,
    val shortcutId: String,
    val userSerial: Long,
    val shortLabel: CharSequence,
    val longLabel: CharSequence?,
    val intentAction: String? = null,
    val intentDataUri: String? = null,
    val mimeType: String? = null,
    val categoriesCsv: String? = null,
    val extrasJson: String? = null
)

data class InstalledActivityOption(
    val title: String,
    val subtitle: String,
    val sourceLabel: String,
    val packageName: String,
    val activityName: String,
    val userSerial: Long
)

class InstalledTargets(private val app: Application) {
    private val launcherApps = app.getSystemService(LauncherApps::class.java)
    private val userManager = app.getSystemService(UserManager::class.java)
    private val importedIconStore = ImportedIconStore(app)

    fun isDefaultLauncher(): Boolean {
        val resolveInfo = app.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            0
        ) ?: return false
        return resolveInfo.activityInfo?.packageName == app.packageName
    }

    fun loadShortcuts(filterPackage: String? = null): List<InstalledShortcutOption> {
        if (!isDefaultLauncher()) {
            return emptyList()
        }

        val results = mutableListOf<InstalledShortcutOption>()
        for (user in launcherApps.profiles) {
            val packages = filterPackage?.let(::listOf) ?: launcherApps
                .getActivityList(null, user)
                .map { it.applicationInfo.packageName }
                .distinct()
            for (packageName in packages) {
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED
                    )
                }
                val shortcuts = runCatching {
                    launcherApps.getShortcuts(query, user)
                }.getOrNull().orEmpty()
                shortcuts
                    .filter { it.isEnabled }
                    .forEach { info ->
                        val label = runCatching {
                            app.packageManager.getApplicationLabel(app.packageManager.getApplicationInfo(packageName, 0)).toString()
                        }.getOrDefault(packageName.substringAfterLast('.'))
                        results += info.toOption(userSerial(user), label)
                    }
            }
        }
        return results.sortedWith(compareBy({ it.title.lowercase() }, { it.subtitle.lowercase() }))
    }

    fun loadActivities(): List<InstalledActivityOption> {
        val results = mutableListOf<InstalledActivityOption>()
        for (user in launcherApps.profiles) {
            launcherApps.getActivityList(null, user).forEach { info ->
                val component = info.componentName ?: return@forEach
                val appLabel = app.packageManager.getApplicationLabel(info.applicationInfo).toString()
                results += InstalledActivityOption(
                    title = info.label?.toString().orEmpty().ifBlank { component.className.substringAfterLast('.') },
                    subtitle = component.packageName,
                    sourceLabel = appLabel,
                    packageName = component.packageName,
                    activityName = component.className,
                    userSerial = userSerial(user)
                )
            }
        }
        return results.sortedWith(compareBy({ it.title.lowercase() }, { it.subtitle.lowercase() }))
    }

    fun startShortcut(packageName: String, shortcutId: String, userSerial: Long): Boolean {
        val user = userHandle(userSerial) ?: return false
        return runCatching {
            launcherApps.startShortcut(
                packageName,
                shortcutId,
                null,
                null,
                user
            )
            true
        }.getOrDefault(false)
    }

    fun importShortcutIcon(packageName: String, shortcutId: String, userSerial: Long, targetId: String): String? {
        val user = userHandle(userSerial) ?: return null
        val shortcut = runCatching {
            launcherApps.getShortcuts(
                LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setShortcutIds(listOf(shortcutId))
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED
                    )
                },
                user
            )
        }.getOrNull().orEmpty().firstOrNull() ?: return null

        val density = app.resources.displayMetrics.densityDpi
            .takeIf { it != DisplayMetrics.DENSITY_DEFAULT }
            ?: DisplayMetrics.DENSITY_XXHIGH
        val drawable = runCatching {
            launcherApps.getShortcutIconDrawable(shortcut, density)
        }.getOrNull() ?: return null
        return importedIconStore.saveShortcutIcon(targetId, drawable)
    }

    fun isImportedShortcutIcon(entry: com.ariy.retrolauncher.model.GameEntry): Boolean {
        val packageName = entry.packageName ?: return false
        val id = entry.shortcutId ?: return false
        val user = userHandle(entry.userSerial ?: 0L) ?: return false
        val info = runCatching { launcherApps.getShortcuts(LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setShortcutIds(listOf(id))
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED)
        }, user)?.firstOrNull() }.getOrNull() ?: return false
        val drawable = runCatching { launcherApps.getShortcutIconDrawable(info, app.resources.displayMetrics.densityDpi) }
            .getOrNull() ?: return false
        return importedIconStore.matchesShortcutIcon(entry.id, entry.imageUri, drawable)
    }

    fun startActivity(componentName: ComponentName, userSerial: Long): Boolean {
        val user = userHandle(userSerial) ?: Process.myUserHandle()
        return runCatching {
            launcherApps.startMainActivity(componentName, user, null, null)
            true
        }.getOrDefault(false)
    }

    fun importActivityIcon(entry: com.ariy.retrolauncher.model.GameEntry): String? {
        val user = userHandle(entry.userSerial ?: 0L) ?: return null
        val info = launcherApps.getActivityList(entry.packageName, user)
            .firstOrNull { it.componentName.className == entry.activityName } ?: return null
        return runCatching { importedIconStore.saveShortcutIcon(entry.id, info.getIcon(app.resources.displayMetrics.densityDpi)) }.getOrNull()
    }

    fun userSerial(user: UserHandle): Long = userManager.getSerialNumberForUser(user)

    private fun userHandle(serial: Long): UserHandle? =
        launcherApps.profiles.firstOrNull { userSerial(it) == serial }
}

private fun ShortcutInfo.toOption(userSerial: Long, sourceLabel: String): InstalledShortcutOption {
    val title = longLabel?.toString().orEmpty()
        .ifBlank { shortLabel?.toString().orEmpty() }
        .ifBlank { id }
    val launchIntent = intents?.lastOrNull()
    return InstalledShortcutOption(
        title = title,
        subtitle = `package`,
        sourceLabel = sourceLabel,
        packageName = `package`,
        shortcutId = id,
        userSerial = userSerial,
        shortLabel = shortLabel ?: id,
        longLabel = longLabel,
        intentAction = launchIntent?.action,
        intentDataUri = launchIntent?.dataString,
        mimeType = launchIntent?.type,
        categoriesCsv = launchIntent?.categories?.joinToString(","),
        extrasJson = launchIntent?.extras?.toJsonString()
    )
}

private fun Bundle.toJsonString(): String? {
    val json = JSONObject()
    keySet().forEach { key ->
        when (val value = get(key)) {
            null -> Unit
            is Boolean, is Int, is Long, is Double, is Float, is String -> json.put(key, value)
            else -> json.put(key, value.toString())
        }
    }
    return json.takeIf { it.length() > 0 }?.toString()
}
