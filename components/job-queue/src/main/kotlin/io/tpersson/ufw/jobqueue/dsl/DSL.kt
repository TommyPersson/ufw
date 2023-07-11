package io.tpersson.ufw.jobqueue.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.jobqueue.JobHandler
import io.tpersson.ufw.jobqueue.JobQueueComponent
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.RootBuilder.jobQueue(builder: JobQueueComponentBuilder.() -> Unit) {
    components["JobQueue"] = JobQueueComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class JobQueueComponentBuilder(public val components: UFWRegistry) {
    public var handlers: Set<JobHandler<*>> = emptySet()
    public fun build(): JobQueueComponent {
        return JobQueueComponent.create(components.core, components.managed, components.database, handlers)
    }
}

public val UFWRegistry.jobQueue: JobQueueComponent get() = _components["JobQueue"] as JobQueueComponent