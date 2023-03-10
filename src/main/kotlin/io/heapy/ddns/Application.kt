@file:JvmName("Application")
package io.heapy.ddns

suspend fun main() {
    val config = System.getenv()

    if (config["MODE"] == "server") {
        ServerFactory(config).start()
    } else {
        ClientFactory(config).start()
    }
}
