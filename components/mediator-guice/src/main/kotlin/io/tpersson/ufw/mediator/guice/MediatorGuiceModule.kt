package io.tpersson.ufw.mediator.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.admin.AdminRequestsAdminFacade
import io.tpersson.ufw.mediator.admin.AdminRequestsAdminFacadeImpl
import io.tpersson.ufw.mediator.internal.MediatorInternal

public class MediatorGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(MediatorInternal::class.java).toProvider(MediatorInternalProvider::class.java)
            bind(Mediator::class.java).toProvider(MediatorProvider::class.java)

            bind(AdminRequestsAdminFacade::class.java).to(AdminRequestsAdminFacadeImpl::class.java)
        }
    }
}
