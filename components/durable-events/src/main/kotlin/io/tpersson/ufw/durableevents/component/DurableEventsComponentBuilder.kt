package io.tpersson.ufw.durableevents.component

import io.tpersson.ufw.admin.component.admin
import io.tpersson.ufw.admin.component.installAdmin
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacadeImpl
import io.tpersson.ufw.databasequeue.component.installDatabaseQueue
import io.tpersson.ufw.databasequeue.component.databaseQueue
import io.tpersson.ufw.durableevents.admin.DurableEventsAdminModule
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import io.tpersson.ufw.durableevents.handler.internal.DurableEventQueueWorkersManager
import io.tpersson.ufw.durableevents.handler.internal.IncomingEventIngesterImpl
import io.tpersson.ufw.durableevents.handler.internal.SimpleDurableEventHandlersProvider
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.durableevents.publisher.internal.DurableEventPublisherImpl
import io.tpersson.ufw.durableevents.publisher.internal.dao.EventOutboxDAO
import io.tpersson.ufw.durableevents.publisher.internal.managed.EventOutboxNotifier
import io.tpersson.ufw.durableevents.publisher.internal.managed.EventOutboxWorker
import io.tpersson.ufw.durableevents.publisher.transports.DirectOutgoingEventTransport
import io.tpersson.ufw.managed.component.installManaged
import io.tpersson.ufw.managed.component.managed

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

    public override fun build(components: ComponentRegistryInternal): DurableEventsComponentImpl {
        val durableEventHandlersProvider = SimpleDurableEventHandlersProvider(emptySet<DurableEventHandler>().toMutableSet())

        val durableEventQueueWorkersManager = DurableEventQueueWorkersManager(
            workerFactory = components.databaseQueue.databaseQueueWorkerFactory,
            durableEventHandlersProvider = durableEventHandlersProvider,
            components.core.objectMapper
        )

        val eventOutboxDAO = EventOutboxDAO(
            database = components.database.database
        )

        val outboxNotifier = EventOutboxNotifier()

        val publisher = DurableEventPublisherImpl(
            objectMapper = components.core.objectMapper,
            outboxNotifier = outboxNotifier,
            outboxDAO = eventOutboxDAO,
        )

        val ingester = IncomingEventIngesterImpl(
            eventHandlersProvider = durableEventHandlersProvider,
            workItemsDAO = components.databaseQueue.workItemsDAO,
            clock = components.core.clock,
        )

        val directTransport = DirectOutgoingEventTransport(
            ingester = ingester
        )

        val eventOutboxWorker = EventOutboxWorker(
            outboxNotifier = outboxNotifier,
            outboxDAO = eventOutboxDAO,
            unitOfWorkFactory = components.database.unitOfWorkFactory,
            outgoingEventTransport = context.outgoingEventTransport ?: directTransport,
            databaseLocks = components.database.locks
        )

        components.admin.register(
            DurableEventsAdminModule(
                durableEventHandlersProvider = durableEventHandlersProvider,
                databaseQueueAdminFacade = DatabaseQueueAdminFacadeImpl(
                    workItemsDAO = components.databaseQueue.workItemsDAO,
                    workItemFailuresDAO = components.databaseQueue.workItemFailuresDAO,
                    workQueuesDAO = components.databaseQueue.workQueuesDAO,
                    workQueue = components.databaseQueue.workQueueInternal,
                    unitOfWorkFactory = components.database.unitOfWorkFactory,
                    clock = components.core.clock
                )
            )
        )

        components.managed.register(durableEventQueueWorkersManager)
        components.managed.register(eventOutboxWorker)

        return DurableEventsComponentImpl(
            eventPublisher = publisher,
            eventIngester = ingester,
            eventHandlers = durableEventHandlersProvider,
        )
    }
}