package io.tpersson.ufw.durablemessages.component

import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.durablemessages.common.IncomingMessageIngester
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import io.tpersson.ufw.durablemessages.handler.internal.DurableMessageHandlerRegistry
import io.tpersson.ufw.durablemessages.publisher.DurableMessagePublisher
import jakarta.inject.Inject
import jakarta.inject.Singleton

public interface DurableMessagesComponent : Component<DurableMessagesComponent> {
    public val messagePublisher: DurableMessagePublisher
    public val messageIngester: IncomingMessageIngester

    public fun register(handler: DurableMessageHandler)
}

public interface DurableMessagesComponentInternal : DurableMessagesComponent {
    public val messageHandlers: DurableMessageHandlerRegistry
}

@Singleton
public class DurableMessagesComponentImpl @Inject constructor(
    public override val messagePublisher: DurableMessagePublisher,
    public override val messageIngester: IncomingMessageIngester,
    public override val messageHandlers: DurableMessageHandlerRegistry,
) : DurableMessagesComponentInternal {
    init {
        Migrator.registerMigrationScript(
            componentName = "durable_messages",
            scriptLocation = "io/tpersson/ufw/durablemessages/migrations/postgres/liquibase.xml"
        )
    }

    public override fun register(handler: DurableMessageHandler) {
        messageHandlers.add(handler)
    }

    public companion object : ComponentKey<DurableMessagesComponent> {
    }
}

public val ComponentRegistry.durableMessages: DurableMessagesComponent get() = get(DurableMessagesComponentImpl)