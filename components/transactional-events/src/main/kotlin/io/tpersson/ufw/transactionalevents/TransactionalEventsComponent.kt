package io.tpersson.ufw.transactionalevents

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.managed.ManagedComponent
import io.tpersson.ufw.transactionalevents.handler.IncomingEventIngester
import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler
import io.tpersson.ufw.transactionalevents.handler.internal.*
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventFailuresDAO
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAOImpl
import io.tpersson.ufw.transactionalevents.handler.internal.managed.ExpiredEventReaper
import io.tpersson.ufw.transactionalevents.handler.internal.managed.StaleEventRescheduler
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.transactionalevents.publisher.internal.dao.EventOutboxDAO
import io.tpersson.ufw.transactionalevents.publisher.internal.TransactionalEventPublisherImpl
import io.tpersson.ufw.transactionalevents.publisher.TransactionalEventPublisher
import io.tpersson.ufw.transactionalevents.publisher.internal.managed.EventOutboxNotifier
import io.tpersson.ufw.transactionalevents.publisher.internal.managed.EventOutboxWorker
import io.tpersson.ufw.transactionalevents.publisher.transports.DirectOutgoingEventTransport
import jakarta.inject.Inject

public class TransactionalEventsComponent @Inject constructor(
    public val eventPublisher: TransactionalEventPublisher,
    public val eventIngester: IncomingEventIngester,
    internal val eventQueueDAO: EventQueueDAO,
    internal val eventFailuresDAO: EventFailuresDAO,
    internal val eventHandlersProvider: EventHandlersProvider,
    internal val staleEventRescheduler: StaleEventRescheduler,
) {
    public fun registerHandler(handler: TransactionalEventHandler) {
        eventHandlersProvider.add(handler)
    }

    init {
        Migrator.registerMigrationScript(
            componentName = "transactional_events",
            scriptLocation = "io/tpersson/ufw/transactionalevents/migrations/postgres/liquibase.xml"
        )
    }

    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
            managedComponent: ManagedComponent,
            outgoingEventTransport: OutgoingEventTransport?,
            handlers: Set<TransactionalEventHandler>,
            config: TransactionalEventsConfig = TransactionalEventsConfig(),
        ): TransactionalEventsComponent {

            val eventOutboxDAO = EventOutboxDAO(
                database = databaseComponent.database
            )

            val outboxNotifier = EventOutboxNotifier()

            val publisher = TransactionalEventPublisherImpl(
                objectMapper = coreComponent.objectMapper,
                outboxNotifier = outboxNotifier,
                outboxDAO = eventOutboxDAO,
            )

            val eventQueueDAO = EventQueueDAOImpl(
                database = databaseComponent.database
            )

            val eventFailuresDAO = EventFailuresDAO(
                database = databaseComponent.database
            )

            val eventHandlersProvider = SimpleEventHandlersProvider(handlers)

            val eventQueueProvider = EventQueueProviderImpl(
                eventQueueDAO = eventQueueDAO,
                eventFailuresDAO = eventFailuresDAO,
                clock = coreComponent.clock,
                config = config,
            )

            val ingester = IncomingEventIngesterImpl(
                eventHandlersProvider = eventHandlersProvider,
                eventQueueProvider = eventQueueProvider,
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

            val eventQueueProcessor = EventQueueProcessor(
                eventHandlersProvider = eventHandlersProvider,
                eventQueueProvider = eventQueueProvider,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                objectMapper = coreComponent.objectMapper,
                config = config,
                clock = coreComponent.clock
            )

            val staleEventRescheduler = StaleEventRescheduler(
                eventQueueDAO = eventQueueDAO,
                clock = coreComponent.clock,
                config = config
            )

            val expiredEventReaper = ExpiredEventReaper(
                eventQueueDAO = eventQueueDAO,
                clock = coreComponent.clock,
                config = config
            )

            managedComponent.register(eventOutboxWorker)
            managedComponent.register(eventQueueProcessor)
            managedComponent.register(staleEventRescheduler)
            managedComponent.register(expiredEventReaper)

            return TransactionalEventsComponent(
                eventPublisher = publisher,
                eventIngester = ingester,
                eventQueueDAO = eventQueueDAO,
                eventFailuresDAO = eventFailuresDAO,
                eventHandlersProvider = eventHandlersProvider,
                staleEventRescheduler = staleEventRescheduler,
            )
        }
    }
}