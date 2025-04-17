package io.tpersson.ufw.durableevents.guice

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.multibindings.OptionalBinder
import io.tpersson.ufw.durableevents.handler.internal.IncomingEventIngesterImpl
import io.tpersson.ufw.durableevents.DurableEventsComponent
import io.tpersson.ufw.durableevents.DurableEventsComponentImpl
import io.tpersson.ufw.durableevents.DurableEventsComponentInternal
import io.tpersson.ufw.durableevents.admin.DurableEventsAdminModule
import io.tpersson.ufw.durableevents.common.IncomingEventIngester
import io.tpersson.ufw.durableevents.handler.internal.DurableEventHandlersProvider
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.durableevents.publisher.DurableEventPublisher
import io.tpersson.ufw.durableevents.publisher.internal.DurableEventPublisherImpl
import io.tpersson.ufw.durableevents.publisher.transports.DirectOutgoingEventTransport

public class DurableEventsGuiceModule : Module {
    override fun configure(binder: Binder) {

        OptionalBinder.newOptionalBinder(binder, OutgoingEventTransport::class.java)
            .setDefault().to(DirectOutgoingEventTransport::class.java)

        binder.bind(DurableEventPublisher::class.java).to(DurableEventPublisherImpl::class.java)
        binder.bind(DurableEventsComponent::class.java).to(DurableEventsComponentImpl::class.java).asEagerSingleton()
        binder.bind(DurableEventsComponentInternal::class.java).to(DurableEventsComponentImpl::class.java).asEagerSingleton()

        binder.bind(DurableEventHandlersProvider::class.java).to(GuiceDurableEventHandlersProvider::class.java)
        binder.bind(IncomingEventIngester::class.java).to(IncomingEventIngesterImpl::class.java)

        binder.bind(DurableEventsAdminModule::class.java).asEagerSingleton()
    }
}