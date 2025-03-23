package io.tpersson.ufw.durablecaches.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.durablecaches.DurableCaches
import io.tpersson.ufw.durablecaches.DurableCachesComponent
import io.tpersson.ufw.durablecaches.admin.DurableCachesAdminFacade
import io.tpersson.ufw.durablecaches.admin.DurableCachesAdminFacadeImpl
import io.tpersson.ufw.durablecaches.internal.DurableCachesImpl
import io.tpersson.ufw.durablecaches.internal.DurableCachesInternal

public class DurableCachesGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(DurableCaches::class.java).to(DurableCachesImpl::class.java)
            bind(DurableCachesInternal::class.java).to(DurableCachesImpl::class.java)
            bind(DurableCachesAdminFacade::class.java).to(DurableCachesAdminFacadeImpl::class.java)
            bind(DurableCachesComponent::class.java).asEagerSingleton()
        }
    }
}