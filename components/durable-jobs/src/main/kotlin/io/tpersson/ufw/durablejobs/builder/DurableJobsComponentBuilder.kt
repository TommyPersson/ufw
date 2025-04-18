package io.tpersson.ufw.durablejobs.builder

import io.tpersson.ufw.admin.builder.admin
import io.tpersson.ufw.admin.builder.installAdmin
import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.database.builder.database
import io.tpersson.ufw.database.builder.installDatabase
import io.tpersson.ufw.databasequeue.builder.databaseQueue
import io.tpersson.ufw.databasequeue.builder.installDatabaseQueue
import io.tpersson.ufw.durablejobs.DurableJobHandler
import io.tpersson.ufw.durablejobs.DurableJobsComponent
import io.tpersson.ufw.durablejobs.DurableJobsComponentImpl
import io.tpersson.ufw.managed.builder.installManaged
import io.tpersson.ufw.managed.builder.managed

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
