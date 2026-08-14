@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(gradleLibs.plugins.android.application)
    alias(gradleLibs.plugins.compose.compiler)
    alias(gradleLibs.plugins.koin.compiler)
    alias(gradleLibs.plugins.kotlin.serialization)
}

val signingProp = file(project.rootProject.file("signing.properties"))
val appVersionCode = AppConfiguration.versionCode
val appVersionName = AppConfiguration.versionName

android {
    signingConfigs {
        if (signingProp.exists()) {
            val properties = Properties().apply {
                load(FileInputStream(signingProp))
            }
            create("release") {
                storeFile = rootProject.file(properties.getProperty("releaseStoreFile"))
                keyAlias = properties.getProperty("releaseKeyAlias")
                storePassword = properties.getProperty("releaseStorePassword")
                keyPassword = properties.getProperty("releaseKeyPassword")
            }
        }
    }

    namespace = AppConfiguration.appId
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        applicationId = AppConfiguration.applicationId
        minSdk = AppConfiguration.minSdk
        targetSdk = AppConfiguration.targetSdk
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingProp.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
    // https://issuetracker.google.com/issues/260059413
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/*.proto"
        }
    }

    lint {
        checkDependencies = false
        // 当前产品仅面向 ARM Android TV，不支持 ChromeOS/x86 架构。
        disable += "ChromeOsAbiSupport"
    }

    androidComponents {
        beforeVariants(selector().withBuildType("debug")) { variant ->
            variant.enable = false
        }

        onVariants { variant ->
            val buildType = variant.buildType ?: variant.name

            variant.outputs.forEach { output ->
                val abi = output.filters.firstOrNull()?.identifier ?: "universal"
                output.outputFileName.set(
                    "SBV_${appVersionName}_${buildType}_$abi.apk"
                )
            }
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.addAll(
        layout.projectDirectory.file("compose_compiler_config.conf")
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(androidx.activity.compose)
    implementation(androidx.core.ktx)
    implementation(androidx.core.splashscreen)
    implementation(androidx.compose.ui)
    implementation(androidx.compose.ui.tooling.preview)
    implementation(androidx.compose.material3)
    implementation(androidx.compose.tv.material)
    implementation(androidx.datastore.preferences)
    implementation(androidx.lifecycle.runtime.ktx)
    implementation(androidx.media3.common)
    implementation(androidx.media3.datasource.okhttp)
    implementation(androidx.media3.exoplayer)
    implementation(androidx.media3.ui)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.qrcode)
    implementation(project(mapOf("path" to ":akdanmaku")))
    implementation(project(mapOf("path" to ":bili-api")))
}
