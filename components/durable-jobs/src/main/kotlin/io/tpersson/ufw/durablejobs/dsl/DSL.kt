package io.tpersson.ufw.durablejobs.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.core.builder.*
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.dsl.installDatabase
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.databasequeue.dsl.installDatabaseQueue
import io.tpersson.ufw.durablejobs.DurableJobHandler
import io.tpersson.ufw.durablejobs.DurableJobsComponent
import io.tpersson.ufw.durablejobs.DurableJobsComponentImpl
import io.tpersson.ufw.managed.dsl.installManaged
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.Root.installDurableJobs(configure: DurableJobsComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installManaged()
    installDatabase()
    installDatabaseQueue()
    installAdmin()

    val ctx = contexts.getOrPut(DurableJobsComponentImpl) { DurableJobsComponentBuilderContext() }
        .also(configure)

    builders.add(DurableJobsComponentBuilder(ctx))
}

public class DurableJobsComponentBuilderContext : ComponentBuilderContext<DurableJobsComponent> {
    public var durableJobHandlers: Set<DurableJobHandler<*>> = emptySet()
}

public class DurableJobsComponentBuilder(
    public val context: DurableJobsComponentBuilderContext
) : ComponentBuilder<DurableJobsComponentImpl> {

    public override fun build(components: ComponentRegistry): DurableJobsComponentImpl {
        return DurableJobsComponentImpl.create(
            coreComponent = components.core,
            managedComponent = components.managed,
            databaseComponent = components.database,
            databaseQueueComponent = components.databaseQueue,
            adminComponent = components.admin,
            durableJobHandlers = context.durableJobHandlers,
        )
    }
}

public val ComponentRegistry.durableJobs: DurableJobsComponent get() = get(DurableJobsComponentImpl)
