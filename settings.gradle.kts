pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Allow repositories in settings.gradle.kts
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") //  Add JitPack here
    }
}

rootProject.name = "AarogyaNidaan"
include(":app") // Keep module inclusion intact
