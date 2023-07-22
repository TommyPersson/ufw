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
import java.time.Duration

@UfwDslMarker
public fun UFWBuilder.RootBuilder.transactionalEvents(builder: TransactionalEventsComponentBuilder.() -> Unit = {}) {
    components["TransactionalEvents"] =
        TransactionalEventsComponentBuilder(UFWRegistry(components)).also(builder).build()
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
    public var queuePollWaitTime: Duration = TransactionalEventsConfig.default.queuePollWaitTime
    public var watchdogRefreshInterval: Duration = TransactionalEventsConfig.default.watchdogRefreshInterval
    public var stalenessDetectionInterval: Duration = TransactionalEventsConfig.default.stalenessDetectionInterval
    public var stalenessAge: Duration = TransactionalEventsConfig.default.stalenessAge
    public var successfulEventRetention: Duration = TransactionalEventsConfig.default.successfulEventRetention
    public var failedEventRetention: Duration = TransactionalEventsConfig.default.failedEventRetention
    public var expiredEventReapingInterval: Duration = TransactionalEventsConfig.default.expiredEventReapingInterval

    internal fun build(): TransactionalEventsConfig {
        return TransactionalEventsConfig(
            queuePollWaitTime = queuePollWaitTime,
            watchdogRefreshInterval = watchdogRefreshInterval,
            stalenessDetectionInterval = stalenessDetectionInterval,
            stalenessAge = stalenessAge,
            successfulEventRetention = successfulEventRetention,
            failedEventRetention = failedEventRetention,
            expiredEventReapingInterval = expiredEventReapingInterval,
        )
    }
}

public val UFWRegistry.transactionalEvents: TransactionalEventsComponent get() = _components["TransactionalEvents"] as TransactionalEventsComponent