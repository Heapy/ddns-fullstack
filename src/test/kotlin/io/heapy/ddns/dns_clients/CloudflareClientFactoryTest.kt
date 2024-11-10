package io.heapy.ddns.dns_clients

import io.heapy.ddns.client.ClientFactory
import io.heapy.ddns.config
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class CloudflareClientFactoryTest {
    @Test
    fun `verify test token`() = runBlocking {
        val factory = ClientFactory(config())
        val token = factory.cloudflareToken

        val response = factory.cloudflareVerifyToken(token)

        assertEquals(
            response.success,
            true,
        )
    }

    @Test
    fun `get dns record`() = runBlocking {
        val factory = ClientFactory(config())
        val config = factory.cloudflareConfiguration

        val record = factory.cloudflareDnsClient.getRecord(
            name = config.domainName,
            zoneId = config.zoneId,
            token = config.token,
        )

        assertEquals(
            record?.name,
            config.domainName,
        )
    }
}
