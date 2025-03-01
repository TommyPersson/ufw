package io.tpersson.ufw.databasequeue.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent
import io.tpersson.ufw.databasequeue.DatabaseQueueConfig

@UfwDslMarker
public fun UFWBuilder.RootBuilder.databaseQueue(builder: DatabaseQueueComponentBuilder.() -> Unit) {
    components["DatabaseQueue"] = DatabaseQueueComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class DatabaseQueueComponentBuilder(
    private val components: UFWRegistry
) {
    public var config: DatabaseQueueConfig = DatabaseQueueConfig()

    public fun build(): DatabaseQueueComponent {
        return DatabaseQueueComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            config = config,
        )
    }
}

public val UFWRegistry.databaseQueue: DatabaseQueueComponent get() = _components["DatabaseQueue"] as DatabaseQueueComponent