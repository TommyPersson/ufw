package io.tpersson.ufw.mediator

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.ComponentKey
import io.tpersson.ufw.core.dsl.UFWComponent
import io.tpersson.ufw.mediator.admin.AdminRequestsAdminFacadeImpl
import io.tpersson.ufw.mediator.admin.AdminRequestsAdminModule
import io.tpersson.ufw.mediator.internal.MediatorImpl
import io.tpersson.ufw.mediator.internal.SimpleMediatorMiddlewareRegistry
import io.tpersson.ufw.mediator.internal.SimpleMediatorRequestHandlerRegistry
import io.tpersson.ufw.mediator.middleware.cacheable.CacheableMiddleware
import io.tpersson.ufw.mediator.middleware.loggable.LoggableMiddleware
import io.tpersson.ufw.mediator.middleware.retryable.RetryableMiddleware
import io.tpersson.ufw.mediator.middleware.timelimited.TimeLimitedMiddleware
import jakarta.inject.Singleton


public interface MediatorComponent : UFWComponent<MediatorComponent> {
    public val mediator: Mediator

    public fun register(requestHandler: RequestHandler<*, *>)

    public fun register(middleware: Middleware<*, *>)
}

public interface MediatorComponentInternal : MediatorComponent

@Singleton
public class MediatorComponentImpl private constructor(
    public override val mediator: Mediator,
    private val handlerRegistry: MediatorRequestHandlerRegistry,
    private val middlewareRegistry: MediatorMiddlewareRegistry,
) : MediatorComponentInternal {

    public override fun register(requestHandler: RequestHandler<*, *>) {
        handlerRegistry.register(requestHandler)
    }

    public override fun register(middleware: Middleware<*, *>) {
        middlewareRegistry.register(middleware)
    }

    public companion object : ComponentKey<MediatorComponent> {
        public fun create(
            coreComponent: CoreComponent,
            adminComponent: AdminComponent,
        ): MediatorComponentImpl {

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
                meterRegistry = coreComponent.meterRegistry,
                handlerRegistry = handlerRegistry,
                middlewareRegistry = middlewareRegistry
            )

            adminComponent.register(
                AdminRequestsAdminModule(
                    adminFacade = AdminRequestsAdminFacadeImpl(
                        mediator = mediator
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
}
