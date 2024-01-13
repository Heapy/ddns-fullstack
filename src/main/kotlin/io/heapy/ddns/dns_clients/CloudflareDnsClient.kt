package io.heapy.ddns.dns_clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

class CloudflareDnsClient(
    private val httpClient: HttpClient,
    private val configuration: Configuration,
) : DnsClient {
    override suspend fun createOrUpdateRecord(
        ip: String,
    ): String? {
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

        log.info("Creare record response: $response")
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

    private suspend fun getRecord(
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
            .body<GetDnsResponse>()
            .result
            .firstOrNull()
    }

    data class Configuration(
        val zoneId: String,
        val token: String,
        val domainName: String,
        val ttl: Long,
    )

    @Serializable
    data class GetDnsResponse(
        val success: Boolean,
        @SerialName("result_info")
        val resultInfo: ResultInfo,
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

        @Serializable
        data class Result(
            val id: String,
            @SerialName("zone_id")
            val zoneId: String,
            @SerialName("zone_name")
            val zoneName: String,
            val name: String,
            val type: String,
            val content: String,
            val proxiable: Boolean,
            val proxied: Boolean,
            val ttl: Long,
            val priority: Long? = null,
            val locked: Boolean,
            val meta: Meta,
            val comment: String?,
            val tags: List<String>,
            @SerialName("created_on")
            val createdOn: String,
            @SerialName("modified_on")
            val modifiedOn: String,
        ) {
            @Serializable
            data class Meta(
                @SerialName("auto_added")
                val autoAdded: Boolean,
                @SerialName("managed_by_apps")
                val managedByApps: Boolean,
                @SerialName("managed_by_argo_tunnel")
                val managedByArgoTunnel: Boolean,
                val source: String,
            )
        }

        @Serializable
        data class ResultInfo(
            val page: Long,
            @SerialName("per_page")
            val perPage: Long,
            val count: Long,
            @SerialName("total_count")
            val totalCount: Long,
            @SerialName("total_pages")
            val totalPage: Long,
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
