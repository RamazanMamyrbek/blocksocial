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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "blocksocial"

include(":app")
include(":core-model")
include(":core-domain")
include(":core-data")
include(":core-ui")
include(":feature-onboarding")
include(":feature-dashboard")
include(":feature-app-selection")
include(":feature-rules")
include(":feature-history")
include(":feature-settings")

include(":shared-fixtures")
project(":shared-fixtures").projectDir = file("../shared/fixtures-validator")
