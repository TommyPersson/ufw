package io.tpersson.ufw.examples.common.events

import com.fasterxml.jackson.annotation.JsonTypeName
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.EventId
import java.time.Instant

@JsonTypeName("ExampleEventV1")
public data class ExampleEventV1(
    override val id: EventId = EventId(),
    override val timestamp: Instant = Instant.now(),
    val myContent: String,
) : Event