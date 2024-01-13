package io.heapy.ddns.dns_clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

class DigitalOceanDnsClient(
    private val httpClient: HttpClient,
    private val configuration: Configuration,
) : DnsClient {
    override suspend fun createOrUpdateRecord(
        ip: String,
    ): String? {
        val records = getDomainRecords()
        val record = records.domain_records
            .find { it.name == configuration.subdomain }

        return if (record == null) {
            log.info("Creating new record")
            createDomainRecord(ip)
            null
        } else {
            if (record.data == ip) {
                log.info("Record already exists and with up to date ip: $ip")
            } else {
                log.info("Record already exists, updating $record")
                updateDomainRecord(record.id, ip)
            }
            record.data
        }
    }

    private suspend fun getDomainRecords(): Records {
        return httpClient
            .get(doUrl) {
                header("Content-Type", "application/json")
                bearerAuth(configuration.token)
            }
            .body()
    }

    @Serializable
    data class Records(
        val domain_records: List<DomainRecord>,
        val links: Links,
        val meta: Meta,
    ) {
        @Serializable
        data class DomainRecord(
            val id: Long,
            val type: String,
            val `data`: String,
            val ttl: Int,
            val name: String,
            val flags: Int? = null,
            val port: Int? = null,
            val priority: Int? = null,
            val tag: String? = null,
            val weight: Int? = null,
        )

        @Serializable
        class Links

        @Serializable
        data class Meta(
            val total: Int,
        )
    }

    @Serializable
    data class DomainRecord(
        val type: String,
        val name: String,
        val data: String,
        val ttl: Int,
    )

    private suspend fun updateDomainRecord(id: Long, ip: String) {
        httpClient
            .put("$doUrl/$id") {
                header("Content-Type", "application/json")
                bearerAuth(configuration.token)
                setBody(
                    DomainRecord(
                        type = "A",
                        name = configuration.subdomain,
                        ttl = 300,
                        data = ip,
                    )
                )
            }
    }

    private suspend fun createDomainRecord(ip: String) {
        httpClient
            .post(doUrl) {
                header("Content-Type", "application/json")
                bearerAuth(configuration.token)
                setBody(
                    DomainRecord(
                        type = "A",
                        name = configuration.subdomain,
                        ttl = 300,
                        data = ip,
                    )
                )
            }
    }

    data class Configuration(
        val token: String,
        val domainName: String,
        val subdomain: String,
        val ttl: Long,
    )

    private val doUrl: String
        get() = "https://api.digitalocean.com/v2/domains/${configuration.domainName}/records"

    private companion object {
        private val log = LoggerFactory.getLogger(DigitalOceanDnsClient::class.java)
    }
}
