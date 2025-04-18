package io.tpersson.ufw.aggregates.component

import io.tpersson.ufw.aggregates.AggregateFactRepository
import io.tpersson.ufw.aggregates.AggregateRepository
import io.tpersson.ufw.aggregates.internal.AggregateRepositoryProvider
import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.durablemessages.publisher.DurableMessagePublisher
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class AggregatesComponent @Inject constructor(
    public val factRepository: AggregateFactRepository,
    public val eventPublisher: DurableMessagePublisher,
    private val repositoryProvider: AggregateRepositoryProvider,
) : Component<AggregatesComponent> {

    init {
        Migrator.registerMigrationScript(
            componentName = "aggregates",
            scriptLocation = "io/tpersson/ufw/aggregates/migrations/postgres/liquibase.xml"
        )
    }

    public fun register(repository: AggregateRepository<*, *>) {
        repositoryProvider.add(repository)
    }

    public companion object : ComponentKey<AggregatesComponent> {
    }
}

public val ComponentRegistry.aggregates: AggregatesComponent get() = get(AggregatesComponent)