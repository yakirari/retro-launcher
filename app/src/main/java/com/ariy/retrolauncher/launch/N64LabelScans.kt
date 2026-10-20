package com.ariy.retrolauncher.launch

import java.net.URLEncoder
import org.json.JSONObject

/** Reviewed original cartridge scans. Header CRCs keep different regional labels separate. */
internal object N64LabelScans {
    data class Scan(val filename: String, val sha256: String, val sourceWidth: Int, val sourceHeight: Int,
        val left: Int, val top: Int, val width: Int, val height: Int) {
        val url: String get() = "https://archive.org/download/N64_Cartridge_Art_Collection/" +
            "N64_Art_Collection_images.zip/" + URLEncoder.encode(filename, "UTF-8").replace("+", "%20")
    }

    const val MANIFEST = "cartridge-artwork/n64-scans.json"

    /** Fail closed: unreviewed crops, duplicate identities and unsafe filenames never ship. */
    fun parse(text: String): Map<String, Scan> {
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") == 1 && root.getString("platform") == "Nintendo 64")
        val entries = root.getJSONArray("entries")
        return buildMap {
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                if (!entry.optBoolean("reviewed")) continue
                val crop = entry.getJSONArray("crop")
                require(crop.length() == 4)
                val scan = Scan(entry.getString("filename"), entry.getString("sha256"),
                    entry.getInt("sourceWidth"), entry.getInt("sourceHeight"),
                    crop.getInt(0), crop.getInt(1), crop.getInt(2), crop.getInt(3))
                require(scan.filename.endsWith(" Cart.jpg") && !scan.filename.contains('/') &&
                    !scan.filename.contains('\\') && !scan.filename.contains(".."))
                require(scan.sha256.matches(Regex("[0-9a-f]{64}")))
                require(scan.sourceWidth in 64..4096 && scan.sourceHeight in 64..4096)
                require(scan.left >= 0 && scan.top >= 0 && scan.width >= 300 && scan.height >= 350 &&
                    scan.left + scan.width <= scan.sourceWidth && scan.top + scan.height <= scan.sourceHeight)
                require(scan.width.toFloat() / scan.height in 0.8f..0.95f)
                val signatures = entry.getJSONArray("signatures")
                require(signatures.length() > 0)
                for (j in 0 until signatures.length()) {
                    val signature = signatures.getString(j)
                    require(signature.matches(Regex("[0-9A-F]{8}")) && !containsKey(signature))
                    put(signature, scan)
                }
            }
        }
    }
}