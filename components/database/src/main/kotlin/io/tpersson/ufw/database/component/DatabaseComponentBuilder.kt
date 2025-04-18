package io.tpersson.ufw.database.component

import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.database.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.locks.internal.DatabaseLocksDAO
import io.tpersson.ufw.database.locks.internal.DatabaseLocksImpl
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactoryImpl
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
        components: ComponentRegistryInternal,
    ): DatabaseComponent {
        val connectionProvider = ConnectionProviderImpl(
            dataSource = context.dataSource ?: error("dataSource must be set for the database component!")
        )

        val database = Database(
            connectionProvider = connectionProvider,
        )

        val unitOfWorkFactory = UnitOfWorkFactoryImpl(
            connectionProvider = connectionProvider,
        )

        val migrator = Migrator(
            connectionProvider = connectionProvider,
            configProvider = components.core.configProvider,
        )

        val databaseLocks = DatabaseLocksImpl(
            databaseLocksDAO = DatabaseLocksDAO(database),
            clock = components.core.clock
        )

        return DatabaseComponent(
            database = database,
            connectionProvider = connectionProvider,
            unitOfWorkFactory = unitOfWorkFactory,
            locks = databaseLocks,
            migrator = migrator
        )
    }
}


