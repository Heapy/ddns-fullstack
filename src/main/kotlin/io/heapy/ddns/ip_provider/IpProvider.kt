package io.heapy.ddns.ip_provider

interface IpProvider {
    suspend fun getIp(): String
}
