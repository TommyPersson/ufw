package io.tpersson.ufw.transactionalevents.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.transactionalevents.TransactionalEventsComponent
import io.tpersson.ufw.transactionalevents.TransactionalEventsConfig
import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport

@UfwDslMarker
public fun UFWBuilder.RootBuilder.transactionalEvents(builder: TransactionalEventsComponentBuilder.() -> Unit = {}) {
    components["TransactionalEvents"] = TransactionalEventsComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class TransactionalEventsComponentBuilder(
    public val components: UFWRegistry
) {
    public var config: TransactionalEventsConfig = TransactionalEventsConfig()
    public var outgoingEventTransport: OutgoingEventTransport? = null
    public var handlers: Set<TransactionalEventHandler> = emptySet()

    public fun configure(builder: TransactionalEventsConfigBuilder.() -> Unit) {
        config = TransactionalEventsConfigBuilder().also(builder).build()
    }

    public fun build(): TransactionalEventsComponent {
        return TransactionalEventsComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            managedComponent = components.managed,
            outgoingEventTransport = outgoingEventTransport,
            handlers = handlers,
            config = config,
        )
    }
}

@UfwDslMarker
public class TransactionalEventsConfigBuilder {
    public var thing: Boolean = TransactionalEventsConfig.default.thing

    internal fun build(): TransactionalEventsConfig {
        return TransactionalEventsConfig(
            thing = thing,
        )
    }
}

public val UFWRegistry.transactionalEvents: TransactionalEventsComponent get() = _components["TransactionalEvents"] as TransactionalEventsComponent