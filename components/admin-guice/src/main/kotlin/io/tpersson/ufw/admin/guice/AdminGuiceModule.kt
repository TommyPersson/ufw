package io.tpersson.ufw.admin.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.admin.component.AdminComponent
import io.tpersson.ufw.admin.internal.AdminModulesRegistry

public class AdminGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(AdminModulesRegistry::class.java).toProvider(AdminModulesProviderProvider::class.java)
            bind(AdminComponent::class.java).asEagerSingleton()
        }
    }
}
