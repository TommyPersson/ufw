package io.tpersson.ufw.examples.common.events

import io.tpersson.ufw.durableevents.common.DurableEvent
import io.tpersson.ufw.durableevents.common.DurableEventId
import io.tpersson.ufw.durableevents.common.EventDefinition
import java.time.Instant

@EventDefinition(type = "example-event-v1", topic = "example-topic")
public data class ExampleEventV1(
    override val id: DurableEventId = DurableEventId(),
    override val timestamp: Instant = Instant.now(),
    val myContent: String,
) : DurableEvent()