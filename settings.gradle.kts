pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("org\\.tensorflow.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Deleting repositoriesMode line lets Gradle fall back to safe default handling seamlessly
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")