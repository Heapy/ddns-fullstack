package io.heapy.ddns

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.callloging.*
import org.slf4j.event.*
import io.ktor.server.request.*
import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

open class ServerFactory(
    open val config: Map<String, String>,
) {
    data class Configuration(
        val port: Int,
        val host: String,
        val header: String?,
    )

    open val server by lazy {
        embeddedServer(
            factory = CIO,
            port = serverConfiguration.port,
            host = serverConfiguration.host,
            module = module,
        )
    }

    open val module: (Application.() -> Unit) by lazy {
        {
            install(CallLogging) {
                level = Level.INFO
            }

            install(ContentNegotiation) {
                json()
            }

            routing {
                get("/") {
                    val ip = serverConfiguration.header?.let { header ->
                        call.request.header(header)
                    } ?: call.request.local.remoteAddress

                    call.respond(mapOf("ip" to ip))
                }
            }
        }
    }

    open val serverConfiguration by lazy {
        Configuration(
            port = config["PORT"]?.toInt() ?: 8080,
            host = config["HOST"] ?: "0.0.0.0",
            header = config["HEADER"],
        )
    }

    open fun start() {
        server.start(wait = true)
    }
}
