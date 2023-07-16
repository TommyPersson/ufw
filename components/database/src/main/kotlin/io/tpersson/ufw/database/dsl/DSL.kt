package io.tpersson.ufw.database.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.DatabaseComponent
import javax.sql.DataSource

@UfwDslMarker
public fun UFWBuilder.RootBuilder.database(builder: DatabaseComponentBuilder.() -> Unit) {
    components["database"] = DatabaseComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class DatabaseComponentBuilder(private val components: UFWRegistry) {
    public var dataSource: DataSource? = null

    public fun build(): DatabaseComponent {
        return DatabaseComponent.create(
            coreComponent = components.core,
            dataSource = dataSource ?: error("dataSource must be set in 'database { }'!")
        )
    }
}

public val UFWRegistry.database: DatabaseComponent get() = _components["database"] as DatabaseComponent