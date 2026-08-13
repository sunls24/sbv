plugins {
    alias(gradleLibs.plugins.koin.compiler)
    alias(gradleLibs.plugins.kotlin.jvm)
    alias(gradleLibs.plugins.kotlin.serialization)
}

group = "dev.sunls24"

dependencies {
    implementation(project(":bili-api-grpc"))
    implementation(libs.koin.core)
    implementation(libs.koin.annotations)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.serialization.kotlinx)
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}
