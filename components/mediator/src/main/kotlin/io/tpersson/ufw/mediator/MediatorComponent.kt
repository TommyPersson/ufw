package io.tpersson.ufw.mediator

import io.tpersson.ufw.core.CoreComponent

public class MediatorComponent private constructor(
    public val mediator: Mediator
) {
    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            handlers: Set<RequestHandler<*, *>>,
            middlewares: Set<Middleware<*, *>>
        ): MediatorComponent {
            val mediator =  MediatorImpl(
                meterRegistry = coreComponent.meterRegistry,
                handlers = handlers,
                middlewares = middlewares
            )

            return MediatorComponent(mediator)
        }
    }
}
