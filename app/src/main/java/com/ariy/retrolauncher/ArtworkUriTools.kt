package com.ariy.retrolauncher

import android.net.Uri

fun stampArtworkUri(uriString: String, revision: Long = System.currentTimeMillis()): String =
    Uri.parse(uriString)
        .buildUpon()
        .fragment("artwork-rev-$revision")
        .build()
        .toString()

fun resolveArtworkSourceUri(uriString: String): Uri =
    Uri.parse(uriString)
        .buildUpon()
        .fragment(null)
        .build()
