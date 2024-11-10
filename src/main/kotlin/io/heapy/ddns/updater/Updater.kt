package io.heapy.ddns.updater

interface Updater {
    suspend fun start()
}