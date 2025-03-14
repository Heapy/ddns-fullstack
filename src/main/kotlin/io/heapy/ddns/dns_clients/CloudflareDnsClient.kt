package io.heapy.ddns.dns_clients

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

class CloudflareDnsClient(
    private val httpClient: HttpClient,
    private val configuration: Configuration,
    private val verifyToken: CloudflareVerifyToken,
) : DnsClient {
    override suspend fun createOrUpdateRecord(
        ip: String,
    ): String? {
        val verifyTokenResponse = verifyToken(configuration.token)

        if (!verifyTokenResponse.success) {
            log.error("Token verification failed: ${verifyTokenResponse.errors}")
            return null
        }

        val record = getRecord(
            name = configuration.domainName,
            zoneId = configuration.zoneId,
            token = configuration.token,
        )
        return if (record != null) {
            if (record.content == ip) {
                log.info("Record already exists and with up to date ip: $ip")
            } else {
                log.info("Updating record with id=${record.id}")
                updateRecord(
                    id = record.id,
                    ip = ip,
                    name = configuration.domainName,
                    zoneId = configuration.zoneId,
                    token = configuration.token,
                    ttl = configuration.ttl,
                )
            }
            record.content
        } else {
            log.info("Creating record with ip=$ip")
            createRecord(
                ip = ip,
                name = configuration.domainName,
                zoneId = configuration.zoneId,
                token = configuration.token,
                ttl = configuration.ttl,
            )
            null
        }
    }

    private suspend fun createRecord(
        ip: String,
        name: String,
        zoneId: String,
        token: String,
        ttl: Long,
        proxied: Boolean = false,
        recordType: String = "A",
    ) {
        val response = httpClient
            .post("https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records") {
                header("Content-Type", "application/json")
                bearerAuth(token)
                setBody(
                    UpsertDnsRecordRequest(
                        type = recordType,
                        name = name,
                        content = ip,
                        ttl = ttl,
                        proxied = proxied,
                        comment = getComment("Created"),
                    )
                )
            }
            .bodyAsText()

        log.info("Create record response: $response")
    }

    private suspend fun updateRecord(
        id: String,
        ip: String,
        name: String,
        zoneId: String,
        token: String,
        ttl: Long,
        proxied: Boolean = false,
        recordType: String = "A",
    ) {
        val response = httpClient
            .put("https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records/$id") {
                header("Content-Type", "application/json")
                bearerAuth(token)
                setBody(
                    UpsertDnsRecordRequest(
                        type = recordType,
                        name = name,
                        content = ip,
                        ttl = ttl,
                        proxied = proxied,
                        comment = getComment("Updated"),
                    )
                )
            }
            .bodyAsText()

        log.info("Update record response: $response")
    }

    private fun getComment(type: String): String {
        return "$type by io.heapy.ddns-fullstack on ${OffsetDateTime.now()}"
    }

    internal suspend fun getRecord(
        name: String,
        zoneId: String,
        token: String,
    ): GetDnsResponse.Result? {
        return httpClient
            .get("https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records") {
                header("Content-Type", "application/json")
                bearerAuth(token)
                parameter("name", name)
            }
            .bodyAsText()
            .let { text ->
                try {
                    Json.decodeFromString<GetDnsResponse>(text)
                } catch (e: SerializationException) {
                    log.error("Failed to parse response: $text", e)
                    throw e
                }
            }
            .result
            .firstOrNull()
    }

    data class Configuration(
        val zoneId: String,
        val token: String,
        val domainName: String,
        val ttl: Long,
    )

    @OptIn(ExperimentalSerializationApi::class)
    @JsonIgnoreUnknownKeys
    @Serializable
    data class GetDnsResponse(
        val success: Boolean,
        val errors: List<Error>,
        val messages: List<Message>,
        val result: List<Result>,
    ) {
        @Serializable
        data class Message(
            val code: Int,
            val message: String,
        )

        @Serializable
        data class Error(
            val code: Int,
            val message: String,
        )

        @JsonIgnoreUnknownKeys
        @Serializable
        data class Result(
            val id: String,
            val name: String,
            val type: String,
            val content: String,
            val proxiable: Boolean,
            val proxied: Boolean,
            val ttl: Long,
            val comment: String?,
            @SerialName("comment_modified_on")
            val commentModifiedOn: String,
            val tags: List<String>,
            @SerialName("created_on")
            val createdOn: String,
            @SerialName("modified_on")
            val modifiedOn: String,
        )
    }

    @Serializable
    data class UpsertDnsRecordRequest(
        val type: String,
        val name: String,
        val content: String,
        val ttl: Long,
        val proxied: Boolean,
        val comment: String,
    )

    private companion object {
        private val log = LoggerFactory.getLogger(CloudflareDnsClient::class.java)
    }
}
