package io.tpersson.ufw.aggregates

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import jakarta.inject.Inject

public class AggregatesComponent @Inject constructor(
    private val coreComponent: CoreComponent,
    private val databaseComponent: DatabaseComponent
) {
    init {
        Migrator.registerMigrationScript(
            componentName = "aggregates",
            scriptLocation = "io/tpersson/ufw/aggregates/migrations/postgres/liquibase.xml"
        )
    }

    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
        ): AggregatesComponent {

            return AggregatesComponent(coreComponent, databaseComponent)
        }
    }
}