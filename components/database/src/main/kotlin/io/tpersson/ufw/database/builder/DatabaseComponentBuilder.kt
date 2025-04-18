package io.tpersson.ufw.database.builder

import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.database.DatabaseComponent
import javax.sql.DataSource

@UfwDslMarker
public fun UFWBuilder.Root.installDatabase(configure: DatabaseComponentBuilderContext.() -> Unit = {}) {
    installCore()

    val ctx = contexts.getOrPut(DatabaseComponent) { DatabaseComponentBuilderContext() }
        .also(configure)

    builders.add(DatabaseComponentBuilder(ctx))
}

public class DatabaseComponentBuilderContext : ComponentBuilderContext<DatabaseComponent> {
    public var dataSource: DataSource? = null
}

public class DatabaseComponentBuilder(
    private val context: DatabaseComponentBuilderContext,
) : ComponentBuilder<DatabaseComponent> {

    public override fun build(
        components: ComponentRegistry,
    ): DatabaseComponent {
        return DatabaseComponent.create(
            coreComponent = components.core,
            dataSource = context.dataSource ?: error("dataSource must be set for the database component!")
        )
    }
}

public val ComponentRegistry.database: DatabaseComponent get() = get(DatabaseComponent)


