package io.heapy.ddns.client

import kotlin.time.Duration

data class ClientConfiguration(
    val serverUrl: String,
    val checkPeriod: Duration,
    val requestTimeout: Duration,
    val attemptsBeforeWarning: Int,
    val telegram: TelegramConfiguration?,
)
