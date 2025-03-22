package io.tpersson.ufw.durableevents

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent
import io.tpersson.ufw.durableevents.common.IncomingEventIngester
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
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

public class DurableEventsComponent @Inject constructor(
    public val eventPublisher: DurableEventPublisher,
    public val eventIngester: IncomingEventIngester,
) {
    init {
        Migrator.registerMigrationScript(
            componentName = "durable_events",
            scriptLocation = "io/tpersson/ufw/durableevents/migrations/postgres/liquibase.xml"
        )
    }

    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
            databaseQueueComponent: DatabaseQueueComponent,
            managedComponent: ManagedComponent,
            outgoingEventTransport: OutgoingEventTransport?,
            handlers: Set<DurableEventHandler>,
            config: DurableEventsConfig = DurableEventsConfig(),
        ): DurableEventsComponent {

            val durableEventHandlersProvider = SimpleDurableEventHandlersProvider(emptySet()) // TODO

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

            managedComponent.register(durableEventQueueWorkersManager)

            managedComponent.register(eventOutboxWorker)

            return DurableEventsComponent(
                eventPublisher = publisher,
                eventIngester = ingester,
            )
        }
    }
}