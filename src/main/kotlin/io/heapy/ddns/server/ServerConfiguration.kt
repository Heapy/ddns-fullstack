package io.heapy.ddns.server

data class ServerConfiguration(
    val port: Int,
    val host: String,
    val header: String?,
)
