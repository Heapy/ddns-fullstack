package io.heapy.ddns.updater

import io.heapy.ddns.dns_clients.DnsClient
import io.heapy.ddns.ip_provider.IpProvider
import io.heapy.ddns.notifiers.Notifier
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.time.Duration

class SimpleUpdater(
    private val checkPeriod: Duration,
    private val ipProvider: IpProvider,
    private val notifier: Notifier?,
    private val dnsClients: List<DnsClient>,
) : Updater {
    override suspend fun start() {
        var nextUpdate = ZonedDateTime.now()
        val running = AtomicBoolean(true)

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            log.info("Shutting down updater")
            running.set(false)
        })

        while (running.get()) {
            delay(100)
            try {
                if (ZonedDateTime.now().isAfter(nextUpdate)) {
                    log.info("Updating IP")
                    sync()
                    nextUpdate = ZonedDateTime.now()
                        .plusSeconds(checkPeriod.inWholeSeconds)
                    log.info("Next sync at $nextUpdate")
                }
            } catch (e: Exception) {
                notifier?.notify("Sync failed with error: ${e.message}")
                throw e
            }
        }

        log.info("Updater stopped")
    }

    private var ip = ""

    private suspend fun sync() {
        val newIp = ipProvider.getIp()

        if (newIp != ip) {
            ip = newIp
            log.info("IP changed to $ip")
            dnsClients.forEach {
                val oldIp = it.createOrUpdateRecord(ip)
                if (oldIp != ip) {
                    notifier?.notify(ip)
                }
            }
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(SimpleUpdater::class.java)
    }
}