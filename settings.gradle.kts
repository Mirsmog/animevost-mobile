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
    }
}

rootProject.name = "AnimeVost"

include(":app")
include(":core:domain")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":feature:home")
include(":feature:catalog")
include(":feature:search")
include(":feature:schedule")
include(":feature:detail")
include(":feature:player")
include(":feature:auth")
include(":feature:profile")
