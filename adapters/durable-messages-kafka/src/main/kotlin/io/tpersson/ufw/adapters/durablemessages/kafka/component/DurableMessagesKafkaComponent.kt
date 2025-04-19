package io.tpersson.ufw.adapters.durablemessages.kafka.component

import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.ComponentRegistry

public class DurableMessagesKafkaComponent : Component<DurableMessagesKafkaComponent> {
    public companion object : ComponentKey<DurableMessagesKafkaComponent>
}

public val ComponentRegistry.durableMessagesKafka: DurableMessagesKafkaComponent
    get() = get(DurableMessagesKafkaComponent)