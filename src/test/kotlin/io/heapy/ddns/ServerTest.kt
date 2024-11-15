package io.heapy.ddns

import io.heapy.ddns.server.ServerFactory
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*

class ServerTest {
    @Test
    fun `ip route`() = testApplication {
        val serverFactory = ServerFactory(emptyMap())

        application {
            with(serverFactory.serverModule) {
                install()
            }
        }

        client.post("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"ip":"localhost"}""", bodyAsText())
        }
    }

    @Test
    fun `healthcheck route`() = testApplication {
        val serverFactory = ServerFactory(emptyMap())

        application {
            with(serverFactory.serverModule) {
                install()
            }
        }

        client.get("/healthcheck").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"status":"ok"}""", bodyAsText())
        }
    }
}
