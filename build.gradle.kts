plugins {
    id("com.android.application") version "8.9.1" apply false
    id("com.android.library") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}

// A transitive dependency (materialswitch/documentfile) pulls androidx.core up to 1.17.0,
// which requires compileSdk 36 and ships Kotlin metadata newer than our pinned compiler
// can safely process. Pin back to a known-compatible version across all modules.
subprojects {
    configurations.all {
        resolutionStrategy {
            force("androidx.core:core:1.13.1", "androidx.core:core-ktx:1.13.1")
        }
    }
}
