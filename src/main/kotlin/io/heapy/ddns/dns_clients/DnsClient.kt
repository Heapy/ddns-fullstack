package io.heapy.ddns.dns_clients

interface DnsClient {
    suspend fun createOrUpdateRecord(ip: String)
}
