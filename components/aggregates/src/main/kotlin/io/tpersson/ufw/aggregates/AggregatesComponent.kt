package io.tpersson.ufw.aggregates

import io.tpersson.ufw.aggregates.internal.AggregateFactRepositoryImpl
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.transactionalevents.TransactionalEventsComponent
import io.tpersson.ufw.transactionalevents.publisher.TransactionalEventPublisher
import jakarta.inject.Inject

public class AggregatesComponent @Inject constructor(
    public val factRepository: AggregateFactRepository,
    public val eventPublisher: TransactionalEventPublisher,
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
            transactionalEventsComponent: TransactionalEventsComponent,
        ): AggregatesComponent {
            val factRepository = AggregateFactRepositoryImpl(
                database = databaseComponent.database,
                objectMapper = coreComponent.objectMapper
            )

            val eventPublisher = transactionalEventsComponent.eventPublisher

            return AggregatesComponent(factRepository, eventPublisher)
        }
    }
}