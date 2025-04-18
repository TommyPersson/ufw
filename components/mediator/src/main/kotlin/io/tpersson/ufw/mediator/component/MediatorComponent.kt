package io.tpersson.ufw.mediator.component

import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.MediatorMiddlewareRegistry
import io.tpersson.ufw.mediator.MediatorRequestHandlerRegistry
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.RequestHandler
import jakarta.inject.Inject
import jakarta.inject.Singleton


public interface MediatorComponent : Component<MediatorComponent> {
    public val mediator: Mediator

    public fun register(requestHandler: RequestHandler<*, *>)

    public fun register(middleware: Middleware<*, *>)
}

public interface MediatorComponentInternal : MediatorComponent

@Singleton
public class MediatorComponentImpl @Inject constructor(
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
    }
}

public val ComponentRegistry.mediator: MediatorComponent get() = get(MediatorComponentImpl)