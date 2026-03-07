package kz.fearsom.financiallifev2.server.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerUser(
    val id: String,
    val username: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(val username: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class AuthResponse(
    val success: Boolean = false,
    /** Short-lived JWT (15 min). Used in Authorization: Bearer header. */
    val accessToken: String = "",
    /** Long-lived opaque token (30 days). Used only for /auth/refresh calls. */
    val refreshToken: String = "",
    val userId: String = "",
    val username: String = "",
    val message: String = ""
)

@Serializable
data class GameStateRequest(
    val userId: String,
    val stateJson: String,
    val slotName: String? = null
)

@Serializable
data class GameStateResponse(
    val found: Boolean = false,
    val stateJson: String = ""
)

@Serializable
data class MakeChoiceRequest(
    val sessionId: String,
    val optionId: String
)
