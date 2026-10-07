package com.ariy.retrolauncher.model

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

enum class GameSourceType {
    SHORTCUT,
    ACTIVITY,
    CUSTOM_INTENT
}

data class LaunchTarget(
    val sourceType: GameSourceType,
    val label: String = "",
    val packageName: String? = null,
    val activityName: String? = null,
    val shortcutId: String? = null,
    val userSerial: Long? = null,
    val intentAction: String? = null,
    val intentDataUri: String? = null,
    val mimeType: String? = null,
    val categoriesCsv: String? = null,
    val extrasJson: String? = null
) {
    fun stableKey(): String = listOf(
        sourceType.name,
        label,
        packageName.orEmpty(),
        activityName.orEmpty(),
        shortcutId.orEmpty(),
        userSerial?.toString().orEmpty(),
        intentAction.orEmpty(),
        intentDataUri.orEmpty()
    ).joinToString("|")

    fun toJson(): JSONObject = JSONObject().apply {
        put("sourceType", sourceType.name)
        put("label", label)
        put("packageName", packageName)
        put("activityName", activityName)
        put("shortcutId", shortcutId)
        put("userSerial", userSerial)
        put("intentAction", intentAction)
        put("intentDataUri", intentDataUri)
        put("mimeType", mimeType)
        put("categoriesCsv", categoriesCsv)
        put("extrasJson", extrasJson)
    }

    companion object {
        fun fromJson(json: JSONObject): LaunchTarget = LaunchTarget(
            sourceType = runCatching {
                GameSourceType.valueOf(json.optString("sourceType"))
            }.getOrDefault(GameSourceType.CUSTOM_INTENT),
            label = json.optString("label", ""),
            packageName = json.optString("packageName").ifBlank { null },
            activityName = json.optString("activityName").ifBlank { null },
            shortcutId = json.optString("shortcutId").ifBlank { null },
            userSerial = json.optLong("userSerial", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE },
            intentAction = json.optString("intentAction").ifBlank { null },
            intentDataUri = json.optString("intentDataUri").ifBlank { null },
            mimeType = json.optString("mimeType").ifBlank { null },
            categoriesCsv = json.optString("categoriesCsv").ifBlank { null },
            extrasJson = json.optString("extrasJson").ifBlank { null }
        )
    }
}

data class GameEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String = "",
    val lastPlayedAtEpochMillis: Long? = null,
    val genre: String? = null,
    val platform: String? = null,
    val imageUri: String? = null,
    val sourceType: GameSourceType,
    val packageName: String? = null,
    val activityName: String? = null,
    val shortcutId: String? = null,
    val userSerial: Long? = null,
    val intentAction: String? = null,
    val intentDataUri: String? = null,
    val mimeType: String? = null,
    val categoriesCsv: String? = null,
    val extrasJson: String? = null,
    val authorizedTreeUri: String? = null,
    val alternateTargetsJson: String? = null,
    val preferredTargetKey: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("subtitle", subtitle)
        put("lastPlayedAtEpochMillis", lastPlayedAtEpochMillis)
        put("genre", genre)
        put("platform", platform)
        put("imageUri", imageUri)
        put("sourceType", sourceType.name)
        put("packageName", packageName)
        put("activityName", activityName)
        put("shortcutId", shortcutId)
        put("userSerial", userSerial)
        put("intentAction", intentAction)
        put("intentDataUri", intentDataUri)
        put("mimeType", mimeType)
        put("categoriesCsv", categoriesCsv)
        put("extrasJson", extrasJson)
        put("authorizedTreeUri", authorizedTreeUri)
        put("alternateTargetsJson", alternateTargetsJson)
        put("preferredTargetKey", preferredTargetKey)
    }

    fun primaryTarget(): LaunchTarget = LaunchTarget(
        sourceType = sourceType,
        label = subtitle,
        packageName = packageName,
        activityName = activityName,
        shortcutId = shortcutId,
        userSerial = userSerial,
        intentAction = intentAction,
        intentDataUri = intentDataUri,
        mimeType = mimeType,
        categoriesCsv = categoriesCsv,
        extrasJson = extrasJson
    )

    fun alternateTargets(): List<LaunchTarget> {
        val json = alternateTargetsJson ?: return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return buildList {
            repeat(array.length()) { index ->
                runCatching { LaunchTarget.fromJson(array.getJSONObject(index)) }.getOrNull()?.let(::add)
            }
        }
    }

    fun allTargets(): List<LaunchTarget> = listOf(primaryTarget()) + alternateTargets()

    fun preferredTarget(): LaunchTarget? =
        preferredTargetKey?.let { preferred ->
            allTargets().firstOrNull { it.stableKey() == preferred }
        }

    companion object {
        fun fromJson(json: JSONObject): GameEntry = GameEntry(
            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = json.optString("title", ""),
            subtitle = json.optString("subtitle", ""),
            lastPlayedAtEpochMillis = json.optLong("lastPlayedAtEpochMillis", Long.MIN_VALUE)
                .takeIf { it != Long.MIN_VALUE },
            genre = json.optString("genre").ifBlank { null },
            platform = json.optString("platform").ifBlank { null },
            imageUri = json.optString("imageUri").ifBlank { null },
            sourceType = runCatching {
                GameSourceType.valueOf(json.optString("sourceType"))
            }.getOrDefault(GameSourceType.CUSTOM_INTENT),
            packageName = json.optString("packageName").ifBlank { null },
            activityName = json.optString("activityName").ifBlank { null },
            shortcutId = json.optString("shortcutId").ifBlank { null },
            userSerial = json.optLong("userSerial", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE },
            intentAction = json.optString("intentAction").ifBlank { null },
            intentDataUri = json.optString("intentDataUri").ifBlank { null },
            mimeType = json.optString("mimeType").ifBlank { null },
            categoriesCsv = json.optString("categoriesCsv").ifBlank { null },
            extrasJson = json.optString("extrasJson").ifBlank { null },
            authorizedTreeUri = json.optString("authorizedTreeUri").ifBlank { null },
            alternateTargetsJson = json.optString("alternateTargetsJson").ifBlank { null },
            preferredTargetKey = json.optString("preferredTargetKey").ifBlank { null }
        )
    }
}

fun List<LaunchTarget>.toJsonString(): String = JSONArray().apply {
    forEach { put(it.toJson()) }
}.toString()
