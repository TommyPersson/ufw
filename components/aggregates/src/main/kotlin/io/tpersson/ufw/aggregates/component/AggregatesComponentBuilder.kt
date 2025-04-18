package io.tpersson.ufw.aggregates.component

import io.tpersson.ufw.admin.component.admin
import io.tpersson.ufw.admin.component.installAdmin
import io.tpersson.ufw.aggregates.admin.AggregatesAdminFacadeImpl
import io.tpersson.ufw.aggregates.admin.AggregatesAdminModule
import io.tpersson.ufw.aggregates.internal.AggregateFactRepositoryImpl
import io.tpersson.ufw.aggregates.internal.SimpleAggregateRepositoryProvider
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.durablemessages.component.installDurableMessages
import io.tpersson.ufw.durablemessages.component.durableMessages

@UfwDslMarker
public fun UFWBuilder.Root.installAggregates(configure: AggregatesComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()
    installDurableMessages()
    installAdmin()

    val ctx = contexts.getOrPut(AggregatesComponent) { AggregatesComponentBuilderContext() }
        .also(configure)

    builders.add(AggregatesComponentBuilder(ctx))
}

public class AggregatesComponentBuilderContext : ComponentBuilderContext<AggregatesComponent>

public class AggregatesComponentBuilder(
    private val context: AggregatesComponentBuilderContext
) : ComponentBuilder<AggregatesComponent> {

    public override fun build(components: ComponentRegistryInternal): AggregatesComponent {
        val factRepository = AggregateFactRepositoryImpl(
            database = components.database.database,
            objectMapper = components.core.objectMapper
        )

        val eventPublisher = components.durableMessages.messagePublisher

        val repositoryProvider = SimpleAggregateRepositoryProvider()

        components.admin.register(
            AggregatesAdminModule(
                adminFacade = AggregatesAdminFacadeImpl(
                    factRepository = factRepository,
                    repositoryProvider = repositoryProvider,
                    objectMapper = components.core.objectMapper,
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