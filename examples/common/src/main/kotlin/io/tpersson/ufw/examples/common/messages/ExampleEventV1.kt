package io.tpersson.ufw.examples.common.messages

import io.tpersson.ufw.durablemessages.common.DurableMessage
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.common.MessageDefinition
import java.time.Instant

@MessageDefinition(
    type = "example-event-v1",
    topic = "example-topic",
    description = "A **simple** example event",
)
public data class ExampleEventV1(
    override val id: DurableMessageId = DurableMessageId(),
    override val timestamp: Instant = Instant.now(),
    val myContent: String,
) : DurableMessage()