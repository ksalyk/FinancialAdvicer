package kz.fearsom.financiallifev2.server.auth

object JwtConfig {
    val secret: String   get() = System.getenv("JWT_SECRET") ?: "dev-secret-change-in-production"
    const val ISSUER     = "financelifeline"
    const val AUDIENCE   = "financelifeline-clients"

    /** Short-lived access token: 15 minutes */
    const val ACCESS_TTL_MS  = 15L * 60 * 1000

    /** Long-lived refresh token: 30 days */
    const val REFRESH_TTL_MS = 30L * 24 * 60 * 60 * 1000
}
