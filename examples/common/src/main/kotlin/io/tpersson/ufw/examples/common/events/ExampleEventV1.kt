package io.tpersson.ufw.examples.common.events

import com.fasterxml.jackson.annotation.JsonTypeName
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.EventDefinition
import io.tpersson.ufw.transactionalevents.EventId
import java.time.Instant

@EventDefinition("ExampleEventV1", "example-topic")
public data class ExampleEventV1(
    override val id: EventId = EventId(),
    override val timestamp: Instant = Instant.now(),
    val myContent: String,
) : Event()