package io.tpersson.ufw.transactionalevents.guice

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.multibindings.OptionalBinder
import io.tpersson.ufw.transactionalevents.TransactionalEventsComponent
import io.tpersson.ufw.transactionalevents.publisher.transports.NoOpOutgoingEventTransport
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.transactionalevents.publisher.TransactionalEventPublisher
import io.tpersson.ufw.transactionalevents.publisher.internal.TransactionalEventPublisherImpl

public class TransactionalEventsGuiceModule : Module {
    override fun configure(binder: Binder) {

        OptionalBinder.newOptionalBinder(binder, OutgoingEventTransport::class.java)
            .setDefault().to(NoOpOutgoingEventTransport::class.java)

        binder.bind(TransactionalEventPublisher::class.java).to(TransactionalEventPublisherImpl::class.java)
        binder.bind(TransactionalEventsComponent::class.java).asEagerSingleton()
    }
}