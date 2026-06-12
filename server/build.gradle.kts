plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

// ── Admin SPA bundling ────────────────────────────────────────────────────────
// Copies the :admin wasmJs production bundle into the server classpath so
// Ktor's staticResources("/admin", "admin-ui") can serve it.
//
// Run manually:   ./gradlew :admin:wasmJsBrowserDistribution :server:run
// Full build:     ./gradlew :server:build   (processResources depends on this task)
val copyAdminUi by tasks.registering(Copy::class) {
    group = "build"
    description = "Copies :admin wasmJs bundle into server resources"

    dependsOn(":admin:wasmJsBrowserDistribution")
    from(project(":admin").layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(layout.projectDirectory.dir("src/main/resources/admin-ui"))
}

tasks.named("processResources") {
    dependsOn(copyAdminUi)
}

application {
    mainClass.set("kz.fearsom.financiallifev2.server.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.sessions)
    // staticResources(...) is provided by ktor-server-core (io.ktor.server.http.content) in 3.x —
    // no separate ktor-server-static artifact exists anymore.
    implementation(libs.logback.classic)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    implementation(libs.postgresql)
    implementation(libs.hikari)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.json)
    testImplementation(libs.h2)
}
