package io.tpersson.ufw.databasequeue.dsl

import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.dsl.installDatabase
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installDatabaseQueue(configure: DatabaseQueueComponentBuilderContext.() -> Unit = {}) {
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

    override fun build(components: UFWComponentRegistry): DatabaseQueueComponent {
        return DatabaseQueueComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
        )
    }
}

public val UFWComponentRegistry.databaseQueue: DatabaseQueueComponent get() = get(DatabaseQueueComponent)