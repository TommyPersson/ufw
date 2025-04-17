package io.tpersson.ufw.mediator.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.mediator.MediatorComponent
import io.tpersson.ufw.mediator.MediatorComponentImpl
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.RequestHandler
import io.tpersson.ufw.mediator.middleware.cacheable.CacheableMiddleware
import io.tpersson.ufw.mediator.middleware.loggable.LoggableMiddleware
import io.tpersson.ufw.mediator.middleware.retryable.RetryableMiddleware
import io.tpersson.ufw.mediator.middleware.timelimited.TimeLimitedMiddleware

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installMediator(configure: MediatorComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installAdmin()

    val ctx = contexts.getOrPut(MediatorComponentImpl) { MediatorComponentBuilderContext() }
        .also(configure)

    builders.add(MediatorComponentBuilder(ctx))
}

public class MediatorComponentBuilderContext : ComponentBuilderContext<MediatorComponent> {
}

public class MediatorComponentBuilder(
    private val context: MediatorComponentBuilderContext
) : ComponentBuilder<MediatorComponentImpl> {

    override val dependencies: List<ComponentKey<*>> = listOf(CoreComponent, AdminComponent)

    override fun build(components: UFWComponentRegistry): MediatorComponentImpl {
        return MediatorComponentImpl.create(
            coreComponent = components.core,
            adminComponent = components.admin,
        )
    }
}

public val UFWComponentRegistry.mediator: MediatorComponent get() = get(MediatorComponentImpl)