package io.tpersson.ufw.aggregates.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.aggregates.AggregatesComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.dsl.installDatabase
import io.tpersson.ufw.durableevents.DurableEventsComponentImpl
import io.tpersson.ufw.durableevents.dsl.durableEvents
import io.tpersson.ufw.durableevents.dsl.installDurableEvents

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installAggregates(configure: AggregatesComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()
    installDurableEvents()
    installAdmin()

    val ctx = contexts.getOrPut(AggregatesComponent) { AggregatesComponentBuilderContext() }
        .also(configure)

    builders.add(AggregatesComponentBuilder(ctx))
}

public class AggregatesComponentBuilderContext : ComponentBuilderContext<AggregatesComponent>

public class AggregatesComponentBuilder(
    private val context: AggregatesComponentBuilderContext
) : ComponentBuilder<AggregatesComponent> {

    override val dependencies: List<ComponentKey<*>> = listOf(
        CoreComponent,
        DatabaseComponent,
        DurableEventsComponentImpl,
        AdminComponent
    )

    public override fun build(components: UFWComponentRegistry): AggregatesComponent {
        return AggregatesComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            durableEventsComponent = components.durableEvents,
            adminComponent = components.admin
        )
    }
}

public val UFWComponentRegistry.aggregates: AggregatesComponent get() = get(AggregatesComponent)