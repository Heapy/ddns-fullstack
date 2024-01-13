package io.heapy.ddns.ip_provider

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

class ServerIpProvider(
    private val httpClient: HttpClient,
    private val serverUrl: String,
) : IpProvider {
    @Serializable
    data class Response(
        val ip: String,
    )

    override suspend fun getIp(): String {
        return httpClient.post(serverUrl)
            .body<Response>()
            .ip
    }
}
