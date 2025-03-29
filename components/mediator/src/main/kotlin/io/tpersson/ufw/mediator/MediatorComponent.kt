package io.tpersson.ufw.mediator

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.mediator.admin.AdminRequestsAdminFacadeImpl
import io.tpersson.ufw.mediator.admin.AdminRequestsAdminModule
import io.tpersson.ufw.mediator.internal.MediatorImpl

public class MediatorComponent private constructor(
    public val mediator: Mediator
) {
    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            adminComponent: AdminComponent,
            handlers: Set<RequestHandler<*, *>>,
            middlewares: Set<Middleware<*, *>>
        ): MediatorComponent {
            val mediator = MediatorImpl(
                meterRegistry = coreComponent.meterRegistry,
                handlers = handlers,
                middlewares = middlewares
            )

            adminComponent.register(
                AdminRequestsAdminModule(
                    adminFacade = AdminRequestsAdminFacadeImpl(
                        mediator = mediator
                    )
                )
            )

            return MediatorComponent(mediator)
        }
    }
}
