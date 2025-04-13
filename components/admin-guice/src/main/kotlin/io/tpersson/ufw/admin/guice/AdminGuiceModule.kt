package io.tpersson.ufw.admin.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.internal.AdminModulesProvider

public class AdminGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(AdminModulesProvider::class.java).toProvider(AdminModulesProviderProvider::class.java)
            bind(AdminComponent::class.java).asEagerSingleton()
        }
    }
}
