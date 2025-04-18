package io.tpersson.ufw.durableevents.component

import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.durableevents.common.IncomingEventIngester
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import io.tpersson.ufw.durableevents.handler.internal.DurableEventHandlersProvider
import io.tpersson.ufw.durableevents.publisher.DurableEventPublisher
import jakarta.inject.Inject
import jakarta.inject.Singleton

public interface DurableEventsComponent : Component<DurableEventsComponent> {
    public val eventPublisher: DurableEventPublisher
    public val eventIngester: IncomingEventIngester

    public fun register(handler: DurableEventHandler)
}

public interface DurableEventsComponentInternal : DurableEventsComponent {
    public val eventHandlers: DurableEventHandlersProvider
}

@Singleton
public class DurableEventsComponentImpl @Inject constructor(
    public override val eventPublisher: DurableEventPublisher,
    public override val eventIngester: IncomingEventIngester,
    public override val eventHandlers: DurableEventHandlersProvider,
) : DurableEventsComponentInternal {
    init {
        Migrator.registerMigrationScript(
            componentName = "durable_events",
            scriptLocation = "io/tpersson/ufw/durableevents/migrations/postgres/liquibase.xml"
        )
    }

    public override fun register(handler: DurableEventHandler) {
        eventHandlers.add(handler)
    }

    public companion object : ComponentKey<DurableEventsComponent> {
    }
}

public val ComponentRegistry.durableEvents: DurableEventsComponent get() = get(DurableEventsComponentImpl)