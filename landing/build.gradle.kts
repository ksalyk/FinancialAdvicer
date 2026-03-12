import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        outputModuleName.set("landing")
        browser {
            commonWebpackConfig {
                outputFileName = "landing.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            // Use Compose Multiplatform plugin's extension functions for versions
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Async support for animations and coroutines
            implementation(libs.kotlinx.coroutines.core)
        }
    }

    // Enable strict compilation to catch deprecations and errors early
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
