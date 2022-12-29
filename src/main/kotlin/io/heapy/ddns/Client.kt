package io.heapy.ddns

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

open class ClientFactory(
    open val config: Map<String, String>,
) {
    data class Configuration(
        val token: String,
        val serverUrl: String,
        val checkPeriod: Duration,
        val requestTimeout: Duration,
        val attemptsBeforeWarning: Int,
        val telegram: Telegram?,
        val domain: Domain,
    ) {
        data class Domain(
            val domain: String,
            val recordType: String,
            val subdomain: String,
            val ttl: Int,
        )

        data class Telegram(
            val token: String,
            val chatId: String,
        )
    }

    open val httpClient by lazy {
        HttpClient(Apache) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    open val updater: Updater by lazy {
        SimpleUpdater(
            checkPeriod = configuration.checkPeriod,
            ipProvider = ipProvider,
            notifier = notifier,
            digitalOceanClient = digitalOceanClient,
        )
    }

    open val digitalOceanClient: DigitalOceanClient by lazy {
        DefaultDigitalOceanClient(
            httpClient = httpClient,
            token = configuration.token,
            domainConfig = configuration.domain,
        )
    }

    open val notifier: Notifier? by lazy {
        configuration.telegram?.let { telegram ->
            TelegramNotifier(
                httpClient = httpClient,
                telegram = telegram,
            )
        }
    }

    open val ipProvider: IpProvider by lazy {
        ServerIpProvider(
            httpClient = httpClient,
            serverUrl = configuration.serverUrl,
        )
    }

    open val configuration by lazy {
        Configuration(
            token = config["TOKEN"]
                ?: error("TOKEN is not set"),
            serverUrl = config["SERVER_URL"]
                ?: error("SERVER_URL is not set"),
            checkPeriod = config["CHECK_PERIOD"]?.let(Duration::parse)
                ?: 5.minutes,
            requestTimeout = config["REQUEST_TIMEOUT"]?.let(Duration::parse)
                ?: 30.seconds,
            attemptsBeforeWarning = config["ATTEMPTS_BEFORE_WARNING"]?.toInt() ?: 5,
            telegram = config["TELEGRAM_TOKEN"]?.let { token ->
                Configuration.Telegram(
                    token = token,
                    chatId = config["TELEGRAM_CHAT_ID"]
                        ?: error("TELEGRAM_CHAT_ID is not set"),
                )
            },
            domain = Configuration.Domain(
                domain = config["DOMAIN"]
                    ?: error("DOMAIN is not set"),
                recordType = config["RECORD_TYPE"]
                    ?: "A",
                subdomain = config["SUBDOMAIN"]
                    ?: error("SUBDOMAIN is not set"),
                ttl = config["TTL"]?.toInt()
                    ?: 180,
            ),
        )
    }

    open suspend fun start() {
        updater.start()
    }
}

interface Updater {
    suspend fun start()
}

class SimpleUpdater(
    private val checkPeriod: Duration,
    private val ipProvider: IpProvider,
    private val notifier: Notifier?,
    private val digitalOceanClient: DigitalOceanClient,
) : Updater {
    override suspend fun start() {
        var nextUpdate = ZonedDateTime.now()
        var running = true

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            log.info("Shutting down updater")
            running = false
        })

        while (running) {
            delay(100)
            if (ZonedDateTime.now().isAfter(nextUpdate)) {
                log.info("Updating IP")
                sync()
                nextUpdate = ZonedDateTime.now()
                    .plusSeconds(checkPeriod.inWholeSeconds)
                log.info("Next sync at $nextUpdate")
            }
        }

        log.info("Updater stopped")
    }

    private var IP = ""

    private suspend fun sync() {
        val newIP = ipProvider.getIp()

        if (newIP != IP) {
            IP = newIP
            log.info("IP changed to $IP")
            digitalOceanClient.createOrUpdateRecord(IP)
            notifier?.notify(IP)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SimpleUpdater::class.java)
    }
}

interface IpProvider {
    suspend fun getIp(): String
}

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

interface Notifier {
    suspend fun notify(message: String)
}

class TelegramNotifier(
    private val httpClient: HttpClient,
    private val telegram: ClientFactory.Configuration.Telegram,
) : Notifier {
    @Serializable
    data class SendMessageRequest(
        val chat_id: String,
        val text: String,
    )

    override suspend fun notify(message: String) {
        httpClient.post("https://api.telegram.org/bot${telegram.token}/sendMessage") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(SendMessageRequest(
                chat_id = telegram.chatId,
                text = message,
            ))
        }
    }
}

interface DigitalOceanClient {
    suspend fun createOrUpdateRecord(
        ip: String,
    )
}

class DefaultDigitalOceanClient(
    private val httpClient: HttpClient,
    private val token: String,
    private val domainConfig: ClientFactory.Configuration.Domain,
) : DigitalOceanClient {
    override suspend fun createOrUpdateRecord(
        ip: String,
    ) {
        val records = getDomainRecords()
        val record = records.domain_records
            .find { it.name == domainConfig.subdomain }

        if (record == null) {
            log.info("Creating new record")
            createDomainRecord(ip)
        } else {
            log.info("Record already exists, updating $record")
            updateDomainRecord(record.id, ip)
        }
    }

    private suspend fun getDomainRecords(): Records {
        return httpClient
            .get(doUrl) {
                header("Content-Type", "application/json")
                header("Authorization", "Bearer $token")
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
    )

    private suspend fun updateDomainRecord(id: Long, ip: String) {
        httpClient
            .post("$doUrl/$id") {
                header("Content-Type", "application/json")
                header("Authorization", "Bearer $token")
                setBody(DomainRecord(
                    type = domainConfig.recordType,
                    name = domainConfig.subdomain,
                    data = ip,
                ))
            }
    }

    private suspend fun createDomainRecord(ip: String) {
        httpClient
            .put(doUrl) {
                header("Content-Type", "application/json")
                header("Authorization", "Bearer $token")
                setBody(DomainRecord(
                    type = domainConfig.recordType,
                    name = domainConfig.subdomain,
                    data = ip,
                ))
            }
    }

    private val doUrl: String
        get() = "https://api.digitalocean.com/v2/domains/${domainConfig.domain}/records"

    companion object {
        private val log = LoggerFactory.getLogger(DefaultDigitalOceanClient::class.java)
    }
}
