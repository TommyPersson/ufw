package io.tpersson.ufw.database.dsl

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.database.DatabaseComponent
import javax.sql.DataSource

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installDatabase(configure: DatabaseComponentBuilderContext.() -> Unit = {}) {
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

    override val dependencies: List<ComponentKey<*>> get() = listOf(CoreComponent)

    public override fun build(
        components: UFWComponentRegistry,
    ): DatabaseComponent {
        return DatabaseComponent.create(
            coreComponent = components.core,
            dataSource = context.dataSource ?: error("dataSource must be set for the database component!")
        )
    }
}

public val UFWComponentRegistry.database: DatabaseComponent get() = get(DatabaseComponent)


