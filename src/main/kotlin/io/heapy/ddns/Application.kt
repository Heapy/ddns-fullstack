@file:JvmName("Application")
package io.heapy.ddns

import io.heapy.ddns.server.ServerFactory
import io.heapy.komok.tech.dotenv.dotenv

suspend fun main() {
    val config = dotenv()

    if (config["MODE"] == "server") {
        ServerFactory(config).start()
    } else {
        ClientFactory(config).start()
    }
}
