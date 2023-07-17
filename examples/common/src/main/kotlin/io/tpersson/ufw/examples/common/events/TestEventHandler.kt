package io.tpersson.ufw.examples.common.events

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.transactionalevents.handler.EventContext
import io.tpersson.ufw.transactionalevents.handler.EventHandler
import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler
import jakarta.inject.Inject

public class ExampleEventHandler @Inject constructor(
    private val keyValueStore: KeyValueStore,
) : TransactionalEventHandler() {

    private val logger = createLogger()

    @EventHandler(topic = "example-topic")
    public suspend fun handle(event: ExampleEventV1, context: EventContext): Unit {
        logger.info("Counting: $event")

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