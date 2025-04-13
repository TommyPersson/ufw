package io.tpersson.ufw.durableevents.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.durableevents.DurableEventsComponent
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.RootBuilder.durableEvents(builder: DurableEventsComponentBuilder.() -> Unit = {}) {
    components["DurableEvents"] =
        DurableEventsComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class DurableEventsComponentBuilder(
    public val components: UFWRegistry
) {
    public var outgoingEventTransport: OutgoingEventTransport? = null
    public var handlers: Set<DurableEventHandler> = emptySet()

    public fun build(): DurableEventsComponent {
        return DurableEventsComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            databaseQueueComponent = components.databaseQueue,
            managedComponent = components.managed,
            adminComponent = components.admin,
            outgoingEventTransport = outgoingEventTransport,
            handlers = handlers,
        )
    }
}

public val UFWRegistry.durableEvents: DurableEventsComponent get() = _components["DurableEvents"] as DurableEventsComponent