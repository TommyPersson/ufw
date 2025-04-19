package io.tpersson.ufw.durablemessages.guice

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.multibindings.OptionalBinder
import io.tpersson.ufw.durablemessages.handler.internal.IncomingMessageIngesterImpl
import io.tpersson.ufw.durablemessages.component.DurableMessagesComponent
import io.tpersson.ufw.durablemessages.component.DurableMessagesComponentImpl
import io.tpersson.ufw.durablemessages.component.DurableMessagesComponentInternal
import io.tpersson.ufw.durablemessages.admin.DurableMessagesAdminModule
import io.tpersson.ufw.durablemessages.common.IncomingMessageIngester
import io.tpersson.ufw.durablemessages.handler.internal.DurableMessageHandlerRegistry
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import io.tpersson.ufw.durablemessages.publisher.DurableMessagePublisher
import io.tpersson.ufw.durablemessages.publisher.internal.DurableMessagePublisherImpl
import io.tpersson.ufw.durablemessages.publisher.transports.DirectOutgoingMessageTransport

public class DurableMessagesGuiceModule : Module {
    override fun configure(binder: Binder) {

        OptionalBinder.newOptionalBinder(binder, OutgoingMessageTransport::class.java)
            .setDefault().to(DirectOutgoingMessageTransport::class.java)

        binder.bind(DurableMessagePublisher::class.java).to(DurableMessagePublisherImpl::class.java)
        binder.bind(DurableMessagesComponent::class.java).to(DurableMessagesComponentImpl::class.java).asEagerSingleton()
        binder.bind(DurableMessagesComponentInternal::class.java).to(DurableMessagesComponentImpl::class.java).asEagerSingleton()

        binder.bind(DurableMessageHandlerRegistry::class.java).to(GuiceDurableMessageHandlerRegistry::class.java)
        binder.bind(IncomingMessageIngester::class.java).to(IncomingMessageIngesterImpl::class.java)

        binder.bind(DurableMessagesAdminModule::class.java).asEagerSingleton()
    }
}