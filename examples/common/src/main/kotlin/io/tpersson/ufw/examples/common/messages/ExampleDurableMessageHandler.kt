package io.tpersson.ufw.examples.common.messages

import io.tpersson.ufw.durablemessages.handler.DurableMessageContext
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import io.tpersson.ufw.durablemessages.handler.annotations.MessageHandler
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject

public class ExampleDurableMessageHandler @Inject constructor(
    private val keyValueStore: KeyValueStore,
) : DurableMessageHandler {

    @MessageHandler(topic = "example-topic")
    public suspend fun handle(event: ExampleEventV1, context: DurableMessageContext): Unit {
        context.logger.info("Counting: $event")

        val key = KeyValueStore.Key.of<Int>("num-example-events-processed")

        val entry = keyValueStore.get(key)
        val currentCount = entry?.value ?: 0

        keyValueStore.put(
            key = key,
            value = currentCount + 1,
            expectedVersion = entry?.version,
            unitOfWork = context.unitOfWork
        )
    }
}