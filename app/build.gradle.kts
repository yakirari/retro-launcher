import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val nostalgicOverlayMappings = mapOf(
    "3ds.png" to "nostalgic_overlay_3ds.png",
    "cd.png" to "nostalgic_overlay_cd.png",
    "default.png" to "nostalgic_overlay_default.png",
    "dos.png" to "nostalgic_overlay_dos.png",
    "gb.png" to "nostalgic_overlay_gb.png",
    "gba.png" to "nostalgic_overlay_gba.png",
    "gbc.png" to "nostalgic_overlay_gbc.png",
    "gc.png" to "nostalgic_overlay_gc.png",
    "md.png" to "nostalgic_overlay_md.png",
    "n64.png" to "nostalgic_overlay_n64.png",
    "nds.png" to "nostalgic_overlay_nds.png",
    "nes.png" to "nostalgic_overlay_nes.png",
    "ps1.png" to "nostalgic_overlay_ps1.png",
    "ps2.png" to "nostalgic_overlay_ps2.png",
    "ps3.png" to "nostalgic_overlay_ps3.png",
    "psp.png" to "nostalgic_overlay_psp.png",
    "scummvm.png" to "nostalgic_overlay_scummvm.png",
    "snes.png" to "nostalgic_overlay_snes.png",
    "switch.png" to "nostalgic_overlay_switch.png",
    "wii.png" to "nostalgic_overlay_wii.png"
)

val syncOverlayAssets by tasks.registering {
    group = "assets"
    description = "Syncs overlay and background source art into the app's packaged assets and resources."

    val mameOverlaySourceDir = rootProject.file("overlay_mame")
    val mameOverlayTargetDir = layout.projectDirectory.dir("src/main/assets/overlay_mame").asFile
    val nostalgicOverlayTargetDir = layout.projectDirectory.dir("src/main/res/drawable-nodpi").asFile
    val launcherBackgroundSourceDir = rootProject.file("backgrounds")
    val launcherBackgroundTargetDir = layout.projectDirectory.dir("src/main/assets/backgrounds").asFile
    val supportedBackgroundExtensions = setOf("png", "jpg", "jpeg", "webp")

    inputs.files(fileTree(mameOverlaySourceDir) { include("*.png") })
    inputs.files(nostalgicOverlayMappings.keys.map { sourceName -> rootProject.file("overlay/$sourceName") })
    inputs.files(fileTree(launcherBackgroundSourceDir) {
        include("*.png", "*.jpg", "*.jpeg", "*.webp")
    })
    outputs.dir(mameOverlayTargetDir)
    outputs.dir(launcherBackgroundTargetDir)
    outputs.files(nostalgicOverlayMappings.values.map { targetName -> nostalgicOverlayTargetDir.resolve(targetName) })

    doLast {
        mameOverlayTargetDir.mkdirs()
        nostalgicOverlayTargetDir.mkdirs()
        launcherBackgroundTargetDir.mkdirs()

        val mameOverlaySourceFiles = mameOverlaySourceDir
            .listFiles()
            ?.filter { file -> file.isFile && file.extension.equals("png", ignoreCase = true) }
            .orEmpty()
        val mameOverlaySourceNames = mameOverlaySourceFiles.map { file -> file.name }.toSet()

        mameOverlayTargetDir
            .listFiles()
            ?.filter { file -> file.isFile && file.extension.equals("png", ignoreCase = true) }
            ?.forEach { file ->
                if (file.name !in mameOverlaySourceNames) {
                    file.delete()
                }
            }

        mameOverlaySourceFiles.forEach { sourceFile ->
            copy {
                from(sourceFile)
                into(mameOverlayTargetDir)
            }
        }

        val launcherBackgroundSourceFiles = launcherBackgroundSourceDir
            .listFiles()
            ?.filter { file ->
                file.isFile && file.extension.lowercase() in supportedBackgroundExtensions
            }
            .orEmpty()
        val launcherBackgroundSourceNames = launcherBackgroundSourceFiles.map { file -> file.name }.toSet()

        launcherBackgroundTargetDir
            .listFiles()
            ?.filter { file ->
                file.isFile && file.extension.lowercase() in supportedBackgroundExtensions
            }
            ?.forEach { file ->
                if (file.name !in launcherBackgroundSourceNames) {
                    file.delete()
                }
            }

        launcherBackgroundSourceFiles.forEach { sourceFile ->
            copy {
                from(sourceFile)
                into(launcherBackgroundTargetDir)
            }
        }

        nostalgicOverlayMappings.forEach { (sourceName, targetName) ->
            val sourceFile = rootProject.file("overlay/$sourceName")
            require(sourceFile.isFile) {
                "Missing overlay source file: ${sourceFile.path}"
            }
            copy {
                from(sourceFile)
                into(nostalgicOverlayTargetDir)
                rename { targetName }
            }
        }
    }
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("release-keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.ariy.retrolauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ariy.retrolauncher"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // embedded-rpcsx pulls in materialswitch (JitPack), whose metadata is newer than our pinned Kotlin.
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.named("preBuild") {
    dependsOn(syncOverlayAssets)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.caverock:androidsvg-aar:1.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    testImplementation("junit:junit:4.13.2")

    implementation(project(":embedded-rpcsx"))
}
