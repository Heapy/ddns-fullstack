package io.heapy.ddns.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.slf4j.event.Level

class ServerModule(
    private val serverConfiguration: ServerConfiguration,
) {
    fun Application.install() {
        install(CallLogging) {
            level = Level.INFO
        }

        install(ContentNegotiation) {
            json()
        }

        routing {
            post("/") {
                val ip = serverConfiguration.header?.let { header ->
                    call.request.header(header)
                } ?: call.request.local.remoteAddress

                call.respond(mapOf("ip" to ip))
            }

            get("/healthcheck") {
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}