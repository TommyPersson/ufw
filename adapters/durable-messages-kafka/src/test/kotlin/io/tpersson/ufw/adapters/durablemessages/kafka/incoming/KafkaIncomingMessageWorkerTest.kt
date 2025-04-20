package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.core.configuration.ConfigProvider
import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

internal class KafkaIncomingMessageWorkerTest {

    @Test
    fun `launch - Shall exit if the consumer is not enabled`(): Unit = runBlocking {
        val configProvider = ConfigProvider.empty()
        val subscriber = mock<KafkaConsumerSubscriber>()

        val worker = KafkaIncomingMessageWorker(
            handlerRegistry = mock(),
            batchProcessor = mock(),
            subscriber = subscriber,
            configProvider = configProvider,
        )

        worker.start()

        delay(100)

        verifyNoInteractions(subscriber)
    }
}