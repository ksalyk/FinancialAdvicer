plugins {
    kotlin("multiplatform") version "1.9.20"
    kotlin("android") version "1.9.20"
    id("com.android.library") version "8.2.0"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "com.example"
version = "1.0.0"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            // Ktor Client Core
            implementation("io.ktor:ktor-client-core:2.3.6")

            // Ktor serialization
            implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")

            // Kotlin serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

            // Inspektor - HTTP Inspection Library (Chucker alternative)
            implementation("io.github.shreyashkore:inspektor:0.3.9")

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        }

        androidMain.dependencies {
            // Android HTTP engine for Ktor
            implementation("io.ktor:ktor-client-android:2.3.6")

            // Android Core
            implementation("androidx.appcompat:appcompat:1.6.1")
            implementation("androidx.core:core:1.12.0")

            // Jetpack Compose
            implementation("androidx.compose.ui:ui:1.6.0")
            implementation("androidx.compose.material:material:1.6.0")
            implementation("androidx.compose.material3:material3:1.1.2")
            implementation("androidx.activity:activity-compose:1.8.1")

            // Lifecycle
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
        }

        iosMain.dependencies {
            // iOS HTTP engine for Ktor (Darwin)
            implementation("io.ktor:ktor-client-darwin:2.3.6")
        }
    }
}

android {
    namespace = "com.example.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}
