package io.tpersson.ufw.durableevents

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.builders.ComponentKey
import io.tpersson.ufw.core.builders.Component
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacadeImpl
import io.tpersson.ufw.durableevents.admin.DurableEventsAdminModule
import io.tpersson.ufw.durableevents.common.IncomingEventIngester
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import io.tpersson.ufw.durableevents.handler.internal.DurableEventHandlersProvider
import io.tpersson.ufw.durableevents.handler.internal.DurableEventQueueWorkersManager
import io.tpersson.ufw.durableevents.handler.internal.IncomingEventIngesterImpl
import io.tpersson.ufw.durableevents.handler.internal.SimpleDurableEventHandlersProvider
import io.tpersson.ufw.managed.ManagedComponent
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.durableevents.publisher.DurableEventPublisher
import io.tpersson.ufw.durableevents.publisher.internal.DurableEventPublisherImpl
import io.tpersson.ufw.durableevents.publisher.internal.dao.EventOutboxDAO
import io.tpersson.ufw.durableevents.publisher.internal.managed.EventOutboxNotifier
import io.tpersson.ufw.durableevents.publisher.internal.managed.EventOutboxWorker
import io.tpersson.ufw.durableevents.publisher.transports.DirectOutgoingEventTransport
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
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
            databaseQueueComponent: DatabaseQueueComponent,
            managedComponent: ManagedComponent,
            adminComponent: AdminComponent,
            outgoingEventTransport: OutgoingEventTransport?,
            handlers: Set<DurableEventHandler>,
        ): DurableEventsComponentImpl {

            val durableEventHandlersProvider = SimpleDurableEventHandlersProvider(handlers.toMutableSet())

            val durableEventQueueWorkersManager = DurableEventQueueWorkersManager(
                workerFactory = databaseQueueComponent.databaseQueueWorkerFactory,
                durableEventHandlersProvider = durableEventHandlersProvider,
                coreComponent.objectMapper
            )

            val eventOutboxDAO = EventOutboxDAO(
                database = databaseComponent.database
            )

            val outboxNotifier = EventOutboxNotifier()

            val publisher = DurableEventPublisherImpl(
                objectMapper = coreComponent.objectMapper,
                outboxNotifier = outboxNotifier,
                outboxDAO = eventOutboxDAO,
            )

            val ingester = IncomingEventIngesterImpl(
                eventHandlersProvider = durableEventHandlersProvider,
                workItemsDAO = databaseQueueComponent.workItemsDAO,
                clock = coreComponent.clock,
            )

            val directTransport = DirectOutgoingEventTransport(
                ingester = ingester
            )

            val eventOutboxWorker = EventOutboxWorker(
                outboxNotifier = outboxNotifier,
                outboxDAO = eventOutboxDAO,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                outgoingEventTransport = outgoingEventTransport ?: directTransport,
                databaseLocks = databaseComponent.locks
            )

            adminComponent.register(
                DurableEventsAdminModule(
                    durableEventHandlersProvider = durableEventHandlersProvider,
                    databaseQueueAdminFacade = DatabaseQueueAdminFacadeImpl(
                        workItemsDAO = databaseQueueComponent.workItemsDAO,
                        workItemFailuresDAO = databaseQueueComponent.workItemFailuresDAO,
                        workQueuesDAO = databaseQueueComponent.workQueuesDAO,
                        workQueue = databaseQueueComponent.workQueueInternal,
                        unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                        clock = coreComponent.clock
                    )
                )
            )

            managedComponent.register(durableEventQueueWorkersManager)
            managedComponent.register(eventOutboxWorker)

            return DurableEventsComponentImpl(
                eventPublisher = publisher,
                eventIngester = ingester,
                eventHandlers = durableEventHandlersProvider,
            )
        }
    }
}