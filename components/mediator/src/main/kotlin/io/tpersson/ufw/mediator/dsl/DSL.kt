package io.tpersson.ufw.mediator.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.mediator.MediatorComponent
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.RequestHandler
import io.tpersson.ufw.mediator.middleware.RetryableMiddleware

@UfwDslMarker
public fun UFWBuilder.RootBuilder.mediator(builder: MediatorComponentBuilder.() -> Unit) {
    components["Mediator"] = MediatorComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class MediatorComponentBuilder(public val components: UFWRegistry) {
    public var handlers: Set<RequestHandler<*, *>> = emptySet()
    public var middlewares: Set<Middleware<*, *>> = emptySet()

    public fun build(): MediatorComponent {
        val middlewares = middlewares + setOf(RetryableMiddleware())
        return MediatorComponent.create(handlers, middlewares)
    }
}

public val UFWRegistry.mediator: MediatorComponent get() = _components["Mediator"] as MediatorComponent