pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") // required by RPCSX's materialswitch dependency
    }
}

rootProject.name = "RetroLauncher"
include(":app")
include(":embedded-rpcsx")
