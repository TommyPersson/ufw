package io.tpersson.ufw.durableevents.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.dsl.installDatabase
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.databasequeue.dsl.installDatabaseQueue
import io.tpersson.ufw.durableevents.DurableEventsComponent
import io.tpersson.ufw.durableevents.DurableEventsComponentImpl
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.managed.ManagedComponent
import io.tpersson.ufw.managed.dsl.installManaged
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installDurableEvents(configure: DurableEventsComponentBuilderContext.() -> Unit = {}) {
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

    override val dependencies: List<ComponentKey<*>> = listOf(
        CoreComponent,
        DatabaseComponent,
        DatabaseQueueComponent,
        ManagedComponent,
        AdminComponent
    )

    public override fun build(components: UFWComponentRegistry): DurableEventsComponentImpl {
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

public val UFWComponentRegistry.durableEvents: DurableEventsComponent get() = get(DurableEventsComponentImpl)