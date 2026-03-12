import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

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
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static {
                        add(project.projectDir.path)
                        add(project.projectDir.resolve("src/wasmJsMain/resources").path)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.10.0")
            implementation("org.jetbrains.compose.foundation:foundation:1.10.0")
            implementation("org.jetbrains.compose.material3:material3:1.10.0-alpha05")
            implementation("org.jetbrains.compose.ui:ui:1.10.0")
            implementation("org.jetbrains.compose.components:components-resources:1.10.0")
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
