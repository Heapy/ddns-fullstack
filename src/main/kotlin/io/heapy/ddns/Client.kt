package io.heapy.ddns

open class ClientFactory(
    open val config: Map<String, String>,
) {
    data class Configuration(
        val token: String,
    )

    open fun start() {
        TODO("Not yet implemented")
    }
}
