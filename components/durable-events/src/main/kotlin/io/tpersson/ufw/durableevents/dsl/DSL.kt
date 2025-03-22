package io.tpersson.ufw.durableevents.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.durableevents.DurableEventsComponent
import io.tpersson.ufw.durableevents.DurableEventsConfig
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import java.time.Duration

@UfwDslMarker
public fun UFWBuilder.RootBuilder.durableEvents(builder: DurableEventsComponentBuilder.() -> Unit = {}) {
    components["DurableEvents"] =
        DurableEventsComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class DurableEventsComponentBuilder(
    public val components: UFWRegistry
) {
    public var config: DurableEventsConfig = DurableEventsConfig()
    public var outgoingEventTransport: OutgoingEventTransport? = null
    public var handlers: Set<DurableEventHandler> = emptySet()

    public fun configure(builder: DurableEventsConfigBuilder.() -> Unit) {
        config = DurableEventsConfigBuilder().also(builder).build()
    }

    public fun build(): DurableEventsComponent {
        return DurableEventsComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            databaseQueueComponent = components.databaseQueue,
            managedComponent = components.managed,
            outgoingEventTransport = outgoingEventTransport,
            handlers = handlers,
            config = config,
        )
    }
}

@UfwDslMarker
public class DurableEventsConfigBuilder {
    public var queuePollWaitTime: Duration = DurableEventsConfig.default.queuePollWaitTime
    public var watchdogRefreshInterval: Duration = DurableEventsConfig.default.watchdogRefreshInterval
    public var stalenessDetectionInterval: Duration = DurableEventsConfig.default.stalenessDetectionInterval
    public var stalenessAge: Duration = DurableEventsConfig.default.stalenessAge
    public var successfulEventRetention: Duration = DurableEventsConfig.default.successfulEventRetention
    public var failedEventRetention: Duration = DurableEventsConfig.default.failedEventRetention
    public var expiredEventReapingInterval: Duration = DurableEventsConfig.default.expiredEventReapingInterval
    public var metricMeasurementInterval: Duration = DurableEventsConfig.default.metricMeasurementInterval

    internal fun build(): DurableEventsConfig {
        return DurableEventsConfig(
            queuePollWaitTime = queuePollWaitTime,
            watchdogRefreshInterval = watchdogRefreshInterval,
            stalenessDetectionInterval = stalenessDetectionInterval,
            stalenessAge = stalenessAge,
            successfulEventRetention = successfulEventRetention,
            failedEventRetention = failedEventRetention,
            expiredEventReapingInterval = expiredEventReapingInterval,
            metricMeasurementInterval = metricMeasurementInterval
        )
    }
}

public val UFWRegistry.durableEvents: DurableEventsComponent get() = _components["DurableEvents"] as DurableEventsComponent