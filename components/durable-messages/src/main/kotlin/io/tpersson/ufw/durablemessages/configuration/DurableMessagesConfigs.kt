package io.tpersson.ufw.durablemessages.configuration

import io.tpersson.ufw.core.configuration.ConfigElement
import io.tpersson.ufw.core.configuration.Configs
import java.time.Duration

public object DurableMessagesConfigs {

    public val OutboxWorkerEnabled: ConfigElement<Boolean> = ConfigElement.of(
        "durable-messages",
        "outbox-worker-enabled",
        default = false
    )

    public val OutboxWorkerInterval: ConfigElement<Duration> = ConfigElement.of(
        "durable-messages",
        "outbox-worker-interval",
        default = Duration.ofSeconds(10)
    )

    public val OutboxWorkerBatchSize: ConfigElement<Int> = ConfigElement.of(
        "durable-messages",
        "outbox-worker-batch-size",
        default = 50
    )
}

public val Configs.DurableMessages: DurableMessagesConfigs get() = DurableMessagesConfigs
