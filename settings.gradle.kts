pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "delulutopia"

include(":app")
include(":core:designsystem")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":feature:start")
include(":feature:home")
include(":feature:letters")
