/**
 * File location: settings.gradle.kts (root level)
 */

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.github.com/ShreyashKore/inspektor") }
    }
}

rootProject.name = "kmp-inspektor-demo"
include(":shared")
include(":androidApp")
