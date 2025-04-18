package io.tpersson.ufw.durablejobs.component

import io.tpersson.ufw.admin.component.admin
import io.tpersson.ufw.admin.component.installAdmin
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.databasequeue.component.installDatabaseQueue
import io.tpersson.ufw.databasequeue.component.databaseQueue
import io.tpersson.ufw.durablejobs.DurableJobHandler
import io.tpersson.ufw.managed.component.installManaged
import io.tpersson.ufw.managed.component.managed

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

    public override fun build(components: ComponentRegistryInternal): DurableJobsComponentImpl {
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
