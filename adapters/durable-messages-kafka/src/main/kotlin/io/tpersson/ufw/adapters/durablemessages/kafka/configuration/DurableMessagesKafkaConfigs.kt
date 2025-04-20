package io.tpersson.ufw.adapters.durablemessages.kafka.configuration

import io.tpersson.ufw.core.configuration.ConfigElement
import io.tpersson.ufw.core.configuration.Configs

public object DurableMessagesKafkaConfigs {
    public val Producer: ConfigElement<Map<String, Any>> = ConfigElement.of(
        "durable-messages",
        "kafka",
        "producer",
        default = emptyMap(),
    )

    public val Consumer: ConfigElement<Map<String, Any>> = ConfigElement.of(
        "durable-messages",
        "kafka",
        "consumer",
        default = emptyMap(),
    )

    public val ConsumerEnabled: ConfigElement<Boolean> = ConfigElement.of(
        "durable-messages",
        "kafka",
        "consumer-enabled",
        default = false,
    )
}

public val Configs.DurableMessagesKafka: DurableMessagesKafkaConfigs get() = DurableMessagesKafkaConfigs