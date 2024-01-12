package io.heapy.ddns.notifiers

interface Notifier {
    suspend fun notify(message: String)
}
