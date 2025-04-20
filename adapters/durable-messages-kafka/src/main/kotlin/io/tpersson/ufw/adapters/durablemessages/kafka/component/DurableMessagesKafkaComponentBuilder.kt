package io.tpersson.ufw.adapters.durablemessages.kafka.component

import io.tpersson.ufw.adapters.durablemessages.kafka.incoming.*
import io.tpersson.ufw.adapters.durablemessages.kafka.outgoing.DefaultKafkaOutgoingMessageConverter
import io.tpersson.ufw.adapters.durablemessages.kafka.outgoing.KafkaOutgoingMessageConverter
import io.tpersson.ufw.adapters.durablemessages.kafka.outgoing.KafkaOutgoingMessageTransport
import io.tpersson.ufw.adapters.durablemessages.kafka.outgoing.KafkaProducerFactoryImpl
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.CoreComponent
import io.tpersson.ufw.core.component.CoreComponentBuilderContext
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.ComponentBuilder
import io.tpersson.ufw.core.components.ComponentBuilderContext
import io.tpersson.ufw.core.components.ComponentRegistryInternal
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.durablemessages.component.*
import io.tpersson.ufw.managed.component.managed

@UfwDslMarker
public fun UFWBuilder.Root.installDurableMessagesKafka(configure: DurableMessagesKafkaComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()
    installDurableMessages()

    val ctx = contexts.getOrPut(DurableMessagesKafkaComponent) { DurableMessagesKafkaComponentBuilderContext() }
        .also(configure)

    val coreCtx = contexts.getOrPut(CoreComponent) { CoreComponentBuilderContext() }

    val baseCtx = contexts.getOrPut(DurableMessagesComponentImpl) { DurableMessagesComponentBuilderContext() }
    baseCtx.outgoingMessageTransport = KafkaOutgoingMessageTransport(
        messageConverter = ctx.outgoingMessageConverter,
        kafkaProducerFactory = KafkaProducerFactoryImpl(),
        configProvider = coreCtx.configProvider ?: error("ConfigProvider not set!")

    )

    builders.add(DurableMessagesKafkaComponentBuilder(ctx))
}

public class DurableMessagesKafkaComponentBuilderContext : ComponentBuilderContext<DurableMessagesKafkaComponent> {
    public var kafkaProducerConfig: Map<String, Any> = emptyMap()
    public var kafkaConsumerConfig: Map<String, Any> = emptyMap()

    public var incomingMessageConverter: KafkaIncomingMessageConverter = DefaultKafkaIncomingMessageConverter()
    public var outgoingMessageConverter: KafkaOutgoingMessageConverter = DefaultKafkaOutgoingMessageConverter()
}

public class DurableMessagesKafkaComponentBuilder(
    private val context: DurableMessagesKafkaComponentBuilderContext
) : ComponentBuilder<DurableMessagesKafkaComponent> {

    public override fun build(components: ComponentRegistryInternal): DurableMessagesKafkaComponent {

        val incomingMessageWorker = KafkaIncomingMessageWorker(
            handlerRegistry = (components.durableMessages as DurableMessagesComponentInternal).messageHandlers,
            batchProcessor = KafkaIncomingBatchProcessor(
                incomingMessageConverter = context.incomingMessageConverter,
                messageIngester = components.durableMessages.messageIngester,
                unitOfWorkFactory = components.database.unitOfWorkFactory,
            ),
            subscriber = KafkaConsumerSubscriber(
                consumerFactory = KafkaConsumerFactoryImpl(),
                configProvider = components.core.configProvider,
                appInfoProvider = components.core.appInfoProvider,
            ),
            configProvider = components.core.configProvider,
        )

        components.managed.register(incomingMessageWorker)

        return DurableMessagesKafkaComponent(
        )
    }
}