import com.google.protobuf.gradle.proto

plugins {
    //id("java-library")
    alias(gradleLibs.plugins.google.protobuf)
    alias(gradleLibs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.okhttp)
    api(libs.grpc.protobuf.lite)
    api(libs.grpc.stub)
    api(libs.protobuf.kotlin.lite)
}

sourceSets["main"].proto {
    srcDir("./proto")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.asProvider().get()}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpc.kotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                named("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
            it.plugins {
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    //option("lite")
                }
            }
        }
    }
}
