package io.tpersson.ufw.durablemessages.component

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
import io.tpersson.ufw.durablemessages.admin.DurableMessagesAdminModule
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import io.tpersson.ufw.durablemessages.handler.internal.DurableMessageQueueWorkersManager
import io.tpersson.ufw.durablemessages.handler.internal.IncomingMessageIngesterImpl
import io.tpersson.ufw.durablemessages.handler.internal.SimpleDurableMessageHandlersRegistry
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import io.tpersson.ufw.durablemessages.publisher.internal.DurableMessagePublisherImpl
import io.tpersson.ufw.durablemessages.publisher.internal.dao.MessageOutboxDAO
import io.tpersson.ufw.durablemessages.publisher.internal.managed.MessageOutboxNotifier
import io.tpersson.ufw.durablemessages.publisher.internal.managed.MessageOutboxWorker
import io.tpersson.ufw.durablemessages.publisher.transports.DirectOutgoingMessageTransport
import io.tpersson.ufw.managed.component.installManaged
import io.tpersson.ufw.managed.component.managed

@UfwDslMarker
public fun UFWBuilder.Root.installDurableMessages(configure: DurableMessagesComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()
    installDatabaseQueue()
    installManaged()
    installAdmin()

    val ctx = contexts.getOrPut(DurableMessagesComponentImpl) { DurableMessagesComponentBuilderContext() }
        .also(configure)

    builders.add(DurableMessagesComponentBuilder(ctx))
}

public class DurableMessagesComponentBuilderContext : ComponentBuilderContext<DurableMessagesComponent> {
    /**
     * Determines how outgoing messages are handled.
     *
     * If unset, the [DirectOutgoingMessageTransport] will be used.
     */
    public var outgoingMessageTransport: OutgoingMessageTransport? = null
}

public class DurableMessagesComponentBuilder(
    public val context: DurableMessagesComponentBuilderContext
) : ComponentBuilder<DurableMessagesComponentImpl> {

    public override fun build(components: ComponentRegistryInternal): DurableMessagesComponentImpl {

        val durableMessageHandlersProvider =
            SimpleDurableMessageHandlersRegistry(emptySet<DurableMessageHandler>().toMutableSet())

        val durableMessageQueueWorkersManager = DurableMessageQueueWorkersManager(
            workerFactory = components.databaseQueue.databaseQueueWorkerFactory,
            durableMessageHandlersRegistry = durableMessageHandlersProvider,
            components.core.objectMapper
        )

        val messageOutboxDAO = MessageOutboxDAO(
            database = components.database.database
        )

        val outboxNotifier = MessageOutboxNotifier()

        val publisher = DurableMessagePublisherImpl(
            objectMapper = components.core.objectMapper,
            outboxNotifier = outboxNotifier,
            outboxDAO = messageOutboxDAO,
        )

        val ingester = IncomingMessageIngesterImpl(
            messageHandlersProvider = durableMessageHandlersProvider,
            workItemsDAO = components.databaseQueue.workItemsDAO,
            clock = components.core.clock,
        )

        val outgoingMessageTransport = context.outgoingMessageTransport
            ?: DirectOutgoingMessageTransport(ingester)

        val messageOutboxWorker = MessageOutboxWorker(
            outboxNotifier = outboxNotifier,
            outboxDAO = messageOutboxDAO,
            unitOfWorkFactory = components.database.unitOfWorkFactory,
            outgoingMessageTransport = outgoingMessageTransport,
            databaseLocks = components.database.locks,
            appInfoProvider = components.core.appInfoProvider,
            configProvider = components.core.configProvider,
        )

        components.admin.register(
            DurableMessagesAdminModule(
                durableMessageHandlersRegistry = durableMessageHandlersProvider,
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

        components.managed.register(durableMessageQueueWorkersManager)
        components.managed.register(messageOutboxWorker)

        return DurableMessagesComponentImpl(
            messagePublisher = publisher,
            messageIngester = ingester,
            messageHandlers = durableMessageHandlersProvider,
        )
    }
}