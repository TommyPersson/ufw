package io.tpersson.ufw.mediator.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.mediator.MediatorComponent
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.RequestHandler

@UfwDslMarker
public fun UFWBuilder.RootBuilder.mediator(builder: MediatorComponentBuilder.() -> Unit) {
    components["Mediator"] = MediatorComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class MediatorComponentBuilder(public val components: UFWRegistry) {
    public var handlers: List<RequestHandler<*, *>> = emptyList()
    public var middlewares: List<Middleware<*, *>> = emptyList()

    public fun build(): MediatorComponent {
        return MediatorComponent.create(handlers, middlewares)
    }
}

public val UFWRegistry.mediator: MediatorComponent get() = _components["Mediator"] as MediatorComponent