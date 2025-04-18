package io.tpersson.ufw.durableevents.builder

import io.tpersson.ufw.admin.builder.admin
import io.tpersson.ufw.admin.builder.installAdmin
import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.database.builder.database
import io.tpersson.ufw.database.builder.installDatabase
import io.tpersson.ufw.databasequeue.builder.databaseQueue
import io.tpersson.ufw.databasequeue.builder.installDatabaseQueue
import io.tpersson.ufw.durableevents.DurableEventsComponent
import io.tpersson.ufw.durableevents.DurableEventsComponentImpl
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.managed.builder.installManaged
import io.tpersson.ufw.managed.builder.managed

@UfwDslMarker
public fun UFWBuilder.Root.installDurableEvents(configure: DurableEventsComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()
    installDatabaseQueue()
    installManaged()
    installAdmin()

    val ctx = contexts.getOrPut(DurableEventsComponentImpl) { DurableEventsComponentBuilderContext() }
        .also(configure)

    builders.add(DurableEventsComponentBuilder(ctx))
}

public class DurableEventsComponentBuilderContext : ComponentBuilderContext<DurableEventsComponent> {
    // TODO need to be factory? since an implementation may want to use the Ingester, which is only available later
    public var outgoingEventTransport: OutgoingEventTransport? = null
}

public class DurableEventsComponentBuilder(
    public val context: DurableEventsComponentBuilderContext
) : ComponentBuilder<DurableEventsComponentImpl> {

    public override fun build(components: ComponentRegistry): DurableEventsComponentImpl {
        return DurableEventsComponentImpl.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            databaseQueueComponent = components.databaseQueue,
            managedComponent = components.managed,
            adminComponent = components.admin,
            outgoingEventTransport = context.outgoingEventTransport,
            handlers = emptySet(),
        )
    }
}

public val ComponentRegistry.durableEvents: DurableEventsComponent get() = get(DurableEventsComponentImpl)