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
        maven {
            url = uri("https://jitpack.io") // ✅ Needed for MPAndroidChart, etc.
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // keep if centralizing repos
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io") // ✅ JitPack needed here too!
        }
    }
}

rootProject.name = "soni_innogeek"
include(":app")
