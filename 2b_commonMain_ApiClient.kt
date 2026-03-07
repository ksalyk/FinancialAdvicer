/**
 * File location: src/commonMain/kotlin/com/example/network/ApiClient.kt
 */

package com.example.network

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * Example data class for API response
 */
@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String
)

@Serializable
data class ApiError(
    val message: String,
    val code: Int? = null
)

/**
 * API client for making HTTP requests using the shared NetworkClient.
 * All requests are automatically logged by Inspektor.
 *
 * Example usage:
 * ```
 * val apiClient = ApiClient()
 * val users = apiClient.getUsers().getOrNull()
 * ```
 */
class ApiClient {

    private val client = NetworkClient.httpClient
    private val baseUrl = "https://api.example.com"

    /**
     * Fetch all users
     */
    suspend fun getUsers(): Result<List<User>> = runCatching {
        val response: HttpResponse = client.get("$baseUrl/users")

        when (response.status) {
            HttpStatusCode.OK -> {
                response.body<List<User>>()
            }
            else -> {
                val errorBody = response.body<ApiError>()
                throw Exception("API Error: ${response.status} - ${errorBody.message}")
            }
        }
    }

    /**
     * Fetch a single user by ID
     */
    suspend fun getUser(id: Int): Result<User> = runCatching {
        val response: HttpResponse = client.get("$baseUrl/users/$id")

        when (response.status) {
            HttpStatusCode.OK -> {
                response.body<User>()
            }
            HttpStatusCode.NotFound -> {
                throw Exception("User not found: $id")
            }
            else -> {
                val errorBody = response.body<ApiError>()
                throw Exception("API Error: ${response.status} - ${errorBody.message}")
            }
        }
    }

    /**
     * Create a new user
     */
    suspend fun createUser(user: User): Result<User> = runCatching {
        val response: HttpResponse = client.post("$baseUrl/users") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }

        when (response.status) {
            HttpStatusCode.Created -> {
                response.body<User>()
            }
            else -> {
                val errorBody = response.body<ApiError>()
                throw Exception("Failed to create user: ${response.status} - ${errorBody.message}")
            }
        }
    }

    /**
     * Update an existing user
     */
    suspend fun updateUser(id: Int, user: User): Result<User> = runCatching {
        val response: HttpResponse = client.put("$baseUrl/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                response.body<User>()
            }
            else -> {
                val errorBody = response.body<ApiError>()
                throw Exception("Failed to update user: ${response.status} - ${errorBody.message}")
            }
        }
    }

    /**
     * Delete a user
     */
    suspend fun deleteUser(id: Int): Result<Unit> = runCatching {
        val response: HttpResponse = client.delete("$baseUrl/users/$id")

        when (response.status) {
            HttpStatusCode.NoContent, HttpStatusCode.OK -> {
                // Success
            }
            else -> {
                val errorBody = response.body<ApiError>()
                throw Exception("Failed to delete user: ${response.status} - ${errorBody.message}")
            }
        }
    }
}

/**
 * Extension functions for cleaner error handling
 */
inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> = apply {
    this.getOrNull()?.let(block)
}

inline fun <T> Result<T>.onFailure(block: (Throwable) -> Unit): Result<T> = apply {
    this.exceptionOrNull()?.let(block)
}

inline fun <T, R> Result<T>.map(block: (T) -> R): Result<R> = try {
    Result.success(block(this.getOrThrow()))
} catch (e: Throwable) {
    Result.failure(e)
}
