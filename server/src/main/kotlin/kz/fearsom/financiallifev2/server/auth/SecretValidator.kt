package kz.fearsom.financiallifev2.server.auth

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("SecretValidator")

/**
 * Known dev-default values that must never reach production.
 * If any of these appear when PRODUCTION=true the server refuses to start.
 */
private val INSECURE_DEFAULTS = setOf(
    "dev-secret-change-in-production",
    "dev-admin-key",
    "dev-admin-session-secret-key!!!!",
    "secret",
    "changeme",
    "password",
)

/**
 * Validates that every critical secret is set and not a known insecure default.
 *
 * Detection strategy: the server is considered to be in production mode when
 * the env var PRODUCTION=true (case-insensitive).  In any other configuration
 * (absent, "false", "1", …) the check is skipped so local dev and CI remain
 * unaffected.
 *
 * @throws IllegalStateException if any secret is blank or matches a known default.
 */
fun validateSecretsForProduction() {
    val isProduction = System.getenv("PRODUCTION")?.lowercase() == "true"
    if (!isProduction) {
        log.info("PRODUCTION env var not set to 'true' — skipping secret strength check (dev/test mode)")
        return
    }

    log.info("PRODUCTION=true detected — validating required secrets …")

    data class SecretSpec(val envVar: String, val value: String?)

    val secrets = listOf(
        SecretSpec("JWT_SECRET",      System.getenv("JWT_SECRET")),
        SecretSpec("ADMIN_KEY",       System.getenv("ADMIN_KEY")),
        SecretSpec("SESSION_SECRET",  System.getenv("SESSION_SECRET")),
    )

    val violations = mutableListOf<String>()

    for (spec in secrets) {
        when {
            spec.value.isNullOrBlank() ->
                violations += "${spec.envVar} is not set (blank or missing)"

            spec.value in INSECURE_DEFAULTS ->
                violations += "${spec.envVar} is set to a known insecure dev default"

            spec.value.length < 32 ->
                violations += "${spec.envVar} is too short (${spec.value.length} chars); minimum 32 required in production"
        }
    }

    if (violations.isNotEmpty()) {
        val msg = buildString {
            appendLine("=== PRODUCTION SECRET VALIDATION FAILED ===")
            appendLine("The server refused to start because the following secrets are insecure:")
            violations.forEach { appendLine("  • $it") }
            appendLine()
            appendLine("Fix: set each env var to a cryptographically random string of at least 32 characters.")
            appendLine("     Example: openssl rand -hex 32")
            appendLine("===========================================")
        }
        // Log at ERROR before throwing so the message appears in structured logs.
        log.error(msg)
        throw IllegalStateException(msg)
    }

    log.info("All required secrets are present and non-default — OK")
}
