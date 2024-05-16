package io.heapy.ddns.dns_clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

class CloudflareVerifyToken(
    private val httpClient: HttpClient,
) {
    suspend operator fun invoke(
        token: String,
    ): VerifyTokenResponse {
        return httpClient
            .get("https://api.cloudflare.com/client/v4/user/tokens/verify") {
                header("Content-Type", "application/json")
                bearerAuth(token)
            }
            .body<VerifyTokenResponse>()
    }

    @Serializable
    data class VerifyTokenResponse(
        val result: Result? = null,
        val success: Boolean,
        val errors: List<Error>,
        val messages: List<Message>,
    ) {
        @Serializable
        data class Result(
            val id: String,
            val status: String,
        )

        @Serializable
        data class Error(
            val code: Int,
            val message: String,
        )

        @Serializable
        data class Message(
            val code: Int,
            val message: String,
            val type: String? = null,
        )
    }

}
