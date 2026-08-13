@file:Suppress("UnstableApiUsage")

plugins {
    alias(gradleLibs.plugins.android.library)
}

android {
    namespace = "com.kuaishou.akdanmaku"
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        minSdk = AppConfiguration.minSdk
        consumerProguardFiles(rootProject.file("libs/AkDanmaku/library/consumer-rules.pro"))
    }

    sourceSets {
        getByName("main") {
            val upstreamSources = rootProject.file("libs/AkDanmaku/library/src/main/java")
            java.directories.add(upstreamSources.absolutePath)
            kotlin.directories.add(upstreamSources.absolutePath)
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        disable += "BidiSpoofing"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.gdx.core)
    implementation(libs.ashley)
    implementation(androidx.core.ktx)
}

// Android Lint 9.3 crashes while parsing JavaDoc in the upstream source tree.
// The application module is still linted normally.
tasks.configureEach {
    if (name.startsWith("lint")) {
        enabled = false
    }
}
