package io.tpersson.ufw.examples.common.events

import io.tpersson.ufw.durableevents.handler.DurableEventContext
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import io.tpersson.ufw.durableevents.handler.annotations.EventHandler
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject

public class ExampleDurableEventHandler @Inject constructor(
    private val keyValueStore: KeyValueStore,
) : DurableEventHandler {

    @EventHandler(topic = "example-topic")
    public suspend fun handle(event: ExampleEventV1, context: DurableEventContext): Unit {
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