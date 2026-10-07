package com.ariy.retrolauncher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity

class ActivateLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetIntent = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                Intent(Settings.ACTION_HOME_SETTINGS)
            }

            else -> {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
            }
        }

        startActivity(
            runCatching { targetIntent }
                .getOrElse {
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addCategory(Intent.CATEGORY_DEFAULT)
                    }
                }
        )
        finish()
    }
}
