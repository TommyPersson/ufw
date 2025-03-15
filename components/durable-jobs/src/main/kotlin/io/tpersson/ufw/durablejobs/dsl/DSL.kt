package io.tpersson.ufw.durablejobs.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.durablejobs.DurableJobHandler
import io.tpersson.ufw.durablejobs.DurableJobsComponent
import io.tpersson.ufw.durablejobs.DurableJobsConfig
import io.tpersson.ufw.managed.dsl.managed
import java.time.Duration

@UfwDslMarker
public fun UFWBuilder.RootBuilder.durableJobs(builder: DurableJobsComponentBuilder.() -> Unit) {
    components["DurableJobs"] = DurableJobsComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class DurableJobsComponentBuilder(private val components: UFWRegistry) {
    public var durableJobHandlers: Set<DurableJobHandler<*>> = emptySet()
    public var config: DurableJobsConfig = DurableJobsConfig()

    public fun configure(builder: DurableJobsConfigBuilder.() -> Unit) {
        config = DurableJobsConfigBuilder().also(builder).build()
    }

    internal fun build(): DurableJobsComponent {
        return DurableJobsComponent.create(
            coreComponent = components.core,
            managedComponent = components.managed,
            databaseQueueComponent = components.databaseQueue,
            adminComponent = components._components["Admin"] as? AdminComponent?,
            config = config,
            durableJobHandlers = durableJobHandlers,
        )
    }
}

@UfwDslMarker
public class DurableJobsConfigBuilder {
    // TODO clean up/move stuff to database-queue module
    public var stalenessDetectionInterval: Duration = DurableJobsConfig.Default.stalenessDetectionInterval
    public var stalenessAge: Duration  = DurableJobsConfig.Default.stalenessAge
    public var watchdogRefreshInterval: Duration = DurableJobsConfig.Default.watchdogRefreshInterval
    public var pollWaitTime: Duration  = DurableJobsConfig.Default.pollWaitTime
    public var defaultJobTimeout: Duration  = DurableJobsConfig.Default.defaultJobTimeout
    public var successfulJobRetention: Duration  = DurableJobsConfig.Default.successfulJobRetention
    public var failedJobRetention: Duration  = DurableJobsConfig.Default.failedJobRetention
    public var expiredJobReapingInterval: Duration  = DurableJobsConfig.Default.expiredJobReapingInterval
    public var metricMeasurementInterval: Duration  = DurableJobsConfig.Default.metricMeasurementInterval

    internal fun build(): DurableJobsConfig {
        return DurableJobsConfig(
            stalenessDetectionInterval = stalenessDetectionInterval,
            stalenessAge = stalenessAge,
            pollWaitTime = pollWaitTime,
            defaultJobTimeout = defaultJobTimeout,
            successfulJobRetention = successfulJobRetention,
            failedJobRetention = failedJobRetention,
            expiredJobReapingInterval = expiredJobReapingInterval,
            metricMeasurementInterval = metricMeasurementInterval,
        )
    }
}

public val UFWRegistry.jobQueue: DurableJobsComponent get() = _components["DurableJobs"] as DurableJobsComponent

