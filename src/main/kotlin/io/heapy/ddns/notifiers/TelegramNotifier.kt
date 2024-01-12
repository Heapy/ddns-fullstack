package io.heapy.ddns.notifiers

import io.heapy.ddns.ClientFactory
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class TelegramNotifier(
    private val httpClient: HttpClient,
    private val telegram: ClientFactory.Configuration.Telegram,
) : Notifier {
    @Serializable
    data class SendMessageRequest(
        @SerialName("chat_id")
        val chatId: String,
        val text: String,
    )

    override suspend fun notify(message: String) {
        httpClient.post("https://api.telegram.org/bot${telegram.token}/sendMessage") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                SendMessageRequest(
                    chatId = telegram.chatId,
                    text = message,
                )
            )
        }
    }
}
