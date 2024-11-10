@file:JvmName("Application")
package io.heapy.ddns

import io.heapy.ddns.client.ClientFactory
import io.heapy.ddns.server.ServerFactory
import io.heapy.komok.tech.dotenv.dotenv

suspend fun main() {
    val config = config()

    if (config["MODE"] == "server") {
        ServerFactory(config).start()
    } else {
        ClientFactory(config).start()
    }
}

fun config(): Map<String, String> =
    System.getenv() + dotenv()