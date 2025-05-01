package io.tpersson.ufw.mediator.component

import io.tpersson.ufw.admin.component.admin
import io.tpersson.ufw.admin.component.installAdmin
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.ComponentBuilder
import io.tpersson.ufw.core.components.ComponentBuilderContext
import io.tpersson.ufw.core.components.ComponentRegistryInternal
import io.tpersson.ufw.mediator.admin.AdminRequestsAdminFacadeImpl
import io.tpersson.ufw.mediator.admin.AdminRequestsAdminModule
import io.tpersson.ufw.mediator.internal.MediatorImpl
import io.tpersson.ufw.mediator.internal.SimpleMediatorMiddlewareRegistry
import io.tpersson.ufw.mediator.internal.SimpleMediatorRequestHandlerRegistry
import io.tpersson.ufw.mediator.middleware.cacheable.CacheableMiddleware
import io.tpersson.ufw.mediator.middleware.loggable.LoggableMiddleware
import io.tpersson.ufw.mediator.middleware.retryable.RetryableMiddleware
import io.tpersson.ufw.mediator.middleware.timelimited.TimeLimitedMiddleware

@UfwDslMarker
public fun UFWBuilder.Root.installMediator(configure: MediatorComponentBuilderContext.() -> Unit = {}) {
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

    override fun build(components: ComponentRegistryInternal): MediatorComponentImpl {
        val handlerRegistry = SimpleMediatorRequestHandlerRegistry()

        val middlewareRegistry = SimpleMediatorMiddlewareRegistry(
            setOf(
                RetryableMiddleware(),
                TimeLimitedMiddleware(),
                CacheableMiddleware(),
                LoggableMiddleware(),
            )
        )

        val mediator = MediatorImpl(
            meterRegistry = components.core.meterRegistry,
            handlerRegistry = handlerRegistry,
            middlewareRegistry = middlewareRegistry
        )

        components.admin.register(
            AdminRequestsAdminModule(
                adminFacade = AdminRequestsAdminFacadeImpl(
                    mediator = mediator,
                    objectMapper = components.core.objectMapper,
                )
            )
        )

        return MediatorComponentImpl(
            mediator = mediator,
            handlerRegistry = handlerRegistry,
            middlewareRegistry = middlewareRegistry
        )
    }
}