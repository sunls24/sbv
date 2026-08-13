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
    versionCatalogs {
        create("androidx") { from(files("gradle/androidx.versions.toml")) }
        create("gradleLibs") { from(files("gradle/gradle.versions.toml")) }
    }
}
rootProject.name = "SBV"
include(":app")
include(":akdanmaku")
include(":bili-api")
include(":bili-api-grpc")
