package io.tpersson.ufw.transactionalevents.guice

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.multibindings.OptionalBinder
import io.tpersson.ufw.transactionalevents.TransactionalEventsComponent
import io.tpersson.ufw.transactionalevents.handler.IncomingEventIngester
import io.tpersson.ufw.transactionalevents.handler.internal.*
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAOImpl
import io.tpersson.ufw.transactionalevents.publisher.transports.NoOpOutgoingEventTransport
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.transactionalevents.publisher.TransactionalEventPublisher
import io.tpersson.ufw.transactionalevents.publisher.internal.TransactionalEventPublisherImpl
import io.tpersson.ufw.transactionalevents.publisher.transports.DirectOutgoingEventTransport

public class TransactionalEventsGuiceModule : Module {
    override fun configure(binder: Binder) {

        OptionalBinder.newOptionalBinder(binder, OutgoingEventTransport::class.java)
            .setDefault().to(DirectOutgoingEventTransport::class.java)

        binder.bind(TransactionalEventPublisher::class.java).to(TransactionalEventPublisherImpl::class.java)
        binder.bind(TransactionalEventsComponent::class.java).asEagerSingleton()

        binder.bind(EventQueueDAO::class.java).to(EventQueueDAOImpl::class.java)
        binder.bind(EventHandlersProvider::class.java).to(GuiceEventHandlersProvider::class.java)
        binder.bind(IncomingEventIngester::class.java).to(IncomingEventIngesterImpl::class.java)
        binder.bind(EventQueueProvider::class.java).to(EventQueueProviderImpl::class.java)
    }
}