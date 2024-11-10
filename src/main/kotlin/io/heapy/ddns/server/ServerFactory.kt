package io.heapy.ddns.server

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

/**
 * DDNS Server: Returning request source ip in response
 */
open class ServerFactory(
    open val config: Map<String, String>,
) {
    open val server by lazy {
        embeddedServer(
            factory = CIO,
            port = serverConfiguration.port,
            host = serverConfiguration.host,
        ) {
            with(serverModule) {
                install()
            }
        }
    }

    open val serverModule by lazy {
        ServerModule(
            serverConfiguration = serverConfiguration,
        )
    }

    open val serverConfiguration by lazy {
        ServerConfiguration(
            port = config["PORT"]?.toInt() ?: 8080,
            host = config["HOST"] ?: "0.0.0.0",
            header = config["HEADER"],
        )
    }

    open fun start() {
        server.start(wait = true)
    }
}