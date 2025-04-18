package io.tpersson.ufw.databasequeue.builder

import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.database.builder.database
import io.tpersson.ufw.database.builder.installDatabase
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent

@UfwDslMarker
public fun UFWBuilder.Root.installDatabaseQueue(configure: DatabaseQueueComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()

    val ctx = contexts.getOrPut(DatabaseQueueComponent) { DatabaseQueueComponentBuilderContext() }
        .also(configure)

    builders.add(DatabaseQueueComponentBuilder(ctx))
}


public class DatabaseQueueComponentBuilderContext : ComponentBuilderContext<DatabaseQueueComponent>

public class DatabaseQueueComponentBuilder(
    private val context: DatabaseQueueComponentBuilderContext
) : ComponentBuilder<DatabaseQueueComponent> {

    override fun build(components: ComponentRegistry): DatabaseQueueComponent {
        return DatabaseQueueComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
        )
    }
}

public val ComponentRegistry.databaseQueue: DatabaseQueueComponent get() = get(DatabaseQueueComponent)