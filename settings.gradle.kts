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
        // haho-design (the shared design system) is published to mavenLocal: on this Mac by
        // `./gradlew publishToMavenLocal` in the haho-design repo, on CI by the workflows before
        // the build. Only the id.haho group is resolved from there.
        mavenLocal {
            content { includeGroup("id.haho") }
        }
    }
}

rootProject.name = "MuslimDaily"
include(":app")
include(":baselineprofile")
