package io.tpersson.ufw.cluster.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.cluster.component.ClusterComponent
import io.tpersson.ufw.cluster.admin.ClusterAdminFacade
import io.tpersson.ufw.cluster.admin.ClusterAdminFacadeImpl
import io.tpersson.ufw.cluster.admin.ClusterAdminModule
import io.tpersson.ufw.cluster.internal.ClusterInstancesService
import io.tpersson.ufw.cluster.internal.ClusterInstancesServiceImpl

public class ClusterGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(ClusterAdminFacade::class.java).to(ClusterAdminFacadeImpl::class.java)
            bind(ClusterInstancesService::class.java).to(ClusterInstancesServiceImpl::class.java)
            bind(ClusterComponent::class.java).asEagerSingleton()
            bind(ClusterAdminModule::class.java).asEagerSingleton()

        }
    }
}
