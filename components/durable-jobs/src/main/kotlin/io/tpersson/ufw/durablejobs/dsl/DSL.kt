package io.tpersson.ufw.durablejobs.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.durablejobs.DurableJobHandler
import io.tpersson.ufw.durablejobs.DurableJobsComponent
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.RootBuilder.durableJobs(builder: DurableJobsComponentBuilder.() -> Unit) {
    components["DurableJobs"] = DurableJobsComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class DurableJobsComponentBuilder(public val components: UFWRegistry) {
    public var durableJobHandlers: Set<DurableJobHandler<*>> = emptySet()

    internal fun build(): DurableJobsComponent {
        return DurableJobsComponent.create(
            coreComponent = components.core,
            managedComponent = components.managed,
            databaseComponent = components.database,
            databaseQueueComponent = components.databaseQueue,
            adminComponent = components._components["Admin"] as? AdminComponent?,
            durableJobHandlers = durableJobHandlers,
        )
    }
}

public val UFWRegistry.jobQueue: DurableJobsComponent get() = _components["DurableJobs"] as DurableJobsComponent

