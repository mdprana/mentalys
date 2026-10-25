import java.util.Properties

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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {

    // Load local.properties
    val localPropertiesFile = File(rootDir, "local.properties")
    val localProperties = Properties()
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Cuberto/liquid-swipe-android")
            credentials {
                username = localProperties.getProperty("gpr.user") ?: ""
                password = localProperties.getProperty("gpr.key") ?: ""
            }
        }
        maven {
            name = "GitHubPackagesTarsosDSP"
            url = uri("https://maven.pkg.github.com/JorenSix/TarsosDSP")
            credentials {
                username = localProperties.getProperty("gpr.user") ?: ""
                password = localProperties.getProperty("gpr.key") ?: ""
            }
        }
        maven { url = uri("https://www.jitpack.io") }
        flatDir {
            dirs("libs") // This is where your AAR file is located
        }

    }
}

rootProject.name = "MentalysApp"
include(":app")
