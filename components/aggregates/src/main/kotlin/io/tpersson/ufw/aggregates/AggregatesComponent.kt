package io.tpersson.ufw.aggregates

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.aggregates.admin.AggregatesAdminFacadeImpl
import io.tpersson.ufw.aggregates.admin.AggregatesAdminModule
import io.tpersson.ufw.aggregates.internal.AggregateFactRepositoryImpl
import io.tpersson.ufw.aggregates.internal.AggregateRepositoryProvider
import io.tpersson.ufw.aggregates.internal.SimpleAggregateRepositoryProvider
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.ComponentKey
import io.tpersson.ufw.core.dsl.UFWComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.durableevents.DurableEventsComponent
import io.tpersson.ufw.durableevents.publisher.DurableEventPublisher
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class AggregatesComponent @Inject constructor(
    public val factRepository: AggregateFactRepository,
    public val eventPublisher: DurableEventPublisher,
    private val repositoryProvider: AggregateRepositoryProvider,
) : UFWComponent<AggregatesComponent> {

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
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
            durableEventsComponent: DurableEventsComponent,
            adminComponent: AdminComponent,
        ): AggregatesComponent {
            val factRepository = AggregateFactRepositoryImpl(
                database = databaseComponent.database,
                objectMapper = coreComponent.objectMapper
            )

            val eventPublisher = durableEventsComponent.eventPublisher

            val repositoryProvider = SimpleAggregateRepositoryProvider()

            adminComponent.register(
                AggregatesAdminModule(
                    adminFacade = AggregatesAdminFacadeImpl(
                        factRepository = factRepository,
                        repositoryProvider = repositoryProvider,
                        objectMapper = coreComponent.objectMapper,
                    )
                )
            )

            return AggregatesComponent(
                factRepository = factRepository,
                eventPublisher = eventPublisher,
                repositoryProvider = repositoryProvider
            )
        }
    }
}

