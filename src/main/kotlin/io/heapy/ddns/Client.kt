package io.heapy.ddns

import io.heapy.ddns.dns_clients.CloudflareDnsClient
import io.heapy.ddns.dns_clients.CloudflareVerifyToken
import io.heapy.ddns.dns_clients.DigitalOceanDnsClient
import io.heapy.ddns.dns_clients.DnsClient
import io.heapy.ddns.ip_provider.IpProvider
import io.heapy.ddns.ip_provider.ServerIpProvider
import io.heapy.ddns.updater.SimpleUpdater
import io.heapy.ddns.updater.Updater
import io.heapy.ddns.notifiers.Notifier
import io.heapy.ddns.notifiers.TelegramNotifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
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
                json(Json {
                    ignoreUnknownKeys = true
                })
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
            ttl = configuration.checkPeriod.inWholeSeconds,
        )
    }

    open val cloudflareDnsClient: CloudflareDnsClient by lazy {
        CloudflareDnsClient(
            httpClient = httpClient,
            configuration = cloudflareConfiguration,
            verifyToken = cloudflareVerifyToken,
        )
    }

    open val cloudflareVerifyToken: CloudflareVerifyToken by lazy {
        CloudflareVerifyToken(
            httpClient = httpClient,
        )
    }

    open val cloudflareToken by lazy {
        config["CLOUDFLARE_TOKEN"]
            ?: error("CLOUDFLARE_TOKEN is not set")
    }

    open val cloudflareConfiguration: CloudflareDnsClient.Configuration by lazy {
        CloudflareDnsClient.Configuration(
            zoneId = config["CLOUDFLARE_ZONE_ID"]
                ?: error("CLOUDFLARE_ZONE_ID is not set"),
            domainName = config["CLOUDFLARE_DOMAIN_NAME"]
                ?: error("CLOUDFLARE_DOMAIN_NAME is not set"),
            token = cloudflareToken,
            ttl = checkPeriod.inWholeSeconds,
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

    open val checkPeriod by lazy {
        config["CHECK_PERIOD"]?.let(Duration::parse)
            ?: 5.minutes
    }

    open val configuration by lazy {
        Configuration(
            serverUrl = config["SERVER_URL"]
                ?: error("SERVER_URL is not set"),
            checkPeriod = checkPeriod,
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
