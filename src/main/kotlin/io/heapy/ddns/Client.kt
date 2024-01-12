package io.heapy.ddns

import io.heapy.ddns.dns_clients.CloudflareDnsClient
import io.heapy.ddns.dns_clients.DigitalOceanDnsClient
import io.heapy.ddns.dns_clients.DnsClient
import io.heapy.ddns.notifiers.Notifier
import io.heapy.ddns.notifiers.TelegramNotifier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
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
        val serverUrl: String,
        val checkPeriod: Duration,
        val requestTimeout: Duration,
        val attemptsBeforeWarning: Int,
        val telegram: Telegram?,
    ) {
        data class Telegram(
            val token: String,
            val chatId: String,
        )
    }

    open val httpClient by lazy {
        HttpClient(CIO) {
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
            dnsClients = dnsClients,
        )
    }

    open val dnsClients: List<DnsClient> by lazy {
        buildList {
            if(config["CLOUDFLARE_TOKEN"] != null) add(cloudflareDnsClient)
            if(config["DIGITALOCEAN_TOKEN"] != null) add(digitalOceanDnsClient)
        }
    }

    open val digitalOceanDnsClient: DnsClient by lazy {
        DigitalOceanDnsClient(
            httpClient = httpClient,
            configuration = digitalOceanConfiguration,
        )
    }

    open val digitalOceanConfiguration: DigitalOceanDnsClient.Configuration by lazy {
        DigitalOceanDnsClient.Configuration(
            token = config["DIGITALOCEAN_TOKEN"]
                ?: error("DIGITALOCEAN_TOKEN is not set"),
            domainName = config["DIGITALOCEAN_DOMAIN_NAME"]
                ?: error("DIGITALOCEAN_DOMAIN_NAME is not set"),
            subdomain = config["DIGITALOCEAN_SUBDOMAIN"]
                ?: error("DIGITALOCEAN_SUBDOMAIN is not set"),
        )
    }

    open val cloudflareDnsClient: DnsClient by lazy {
        CloudflareDnsClient(
            httpClient = httpClient,
            configuration = cloudflareConfiguration,
        )
    }

    open val cloudflareConfiguration: CloudflareDnsClient.Configuration by lazy {
        CloudflareDnsClient.Configuration(
            zoneId = config["CLOUDFLARE_ZONE_ID"]
                ?: error("CLOUDFLARE_ZONE_ID is not set"),
            token = config["CLOUDFLARE_TOKEN"]
                ?: error("CLOUDFLARE_TOKEN is not set"),
            domainName = config["CLOUDFLARE_DOMAIN_NAME"]
                ?: error("CLOUDFLARE_DOMAIN_NAME is not set"),
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
    private val dnsClients: List<DnsClient>,
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
            dnsClients.forEach {
                it.createOrUpdateRecord(IP)
            }
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
