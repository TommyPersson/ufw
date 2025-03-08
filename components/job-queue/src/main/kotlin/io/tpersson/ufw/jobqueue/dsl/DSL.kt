package io.tpersson.ufw.jobqueue.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.AdminComponentConfig
import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.jobqueue.JobHandler
import io.tpersson.ufw.jobqueue.JobQueueComponent
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.v2.DurableJobHandler
import io.tpersson.ufw.managed.dsl.managed
import java.time.Duration

@UfwDslMarker
public fun UFWBuilder.RootBuilder.jobQueue(builder: JobQueueComponentBuilder.() -> Unit) {
    components["JobQueue"] = JobQueueComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class JobQueueComponentBuilder(private val components: UFWRegistry) {
    public var handlers: Set<JobHandler<*>> = emptySet()
    public var durableJobHandlers: Set<DurableJobHandler<*>> = emptySet()
    public var config: JobQueueConfig = JobQueueConfig()

    public fun configure(builder: JobQueueConfigBuilder.() -> Unit) {
        config = JobQueueConfigBuilder().also(builder).build()
    }

    internal fun build(): JobQueueComponent {
        return JobQueueComponent.create(
            coreComponent = components.core,
            managedComponent = components.managed,
            databaseComponent = components.database,
            databaseQueueComponent = components.databaseQueue,
            adminComponent = components._components["Admin"] as? AdminComponent?,
            config = config,
            jobHandlers = handlers,
            durableJobHandlers = durableJobHandlers,
        )
    }
}

@UfwDslMarker
public class JobQueueConfigBuilder {
    public var stalenessDetectionInterval: Duration = JobQueueConfig.Default.stalenessDetectionInterval
    public var stalenessAge: Duration  = JobQueueConfig.Default.stalenessAge
    public var watchdogRefreshInterval: Duration = JobQueueConfig.Default.watchdogRefreshInterval
    public var pollWaitTime: Duration  = JobQueueConfig.Default.pollWaitTime
    public var defaultJobTimeout: Duration  = JobQueueConfig.Default.defaultJobTimeout
    public var successfulJobRetention: Duration  = JobQueueConfig.Default.successfulJobRetention
    public var failedJobRetention: Duration  = JobQueueConfig.Default.failedJobRetention
    public var expiredJobReapingInterval: Duration  = JobQueueConfig.Default.expiredJobReapingInterval
    public var metricMeasurementInterval: Duration  = JobQueueConfig.Default.metricMeasurementInterval

    internal fun build(): JobQueueConfig {
        return JobQueueConfig(
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

public val UFWRegistry.jobQueue: JobQueueComponent get() = _components["JobQueue"] as JobQueueComponent

