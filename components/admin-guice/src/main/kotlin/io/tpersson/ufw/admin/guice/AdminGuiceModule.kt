package io.tpersson.ufw.admin.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.AdminComponentConfig
import io.tpersson.ufw.admin.internal.AdminModulesProvider

public class AdminGuiceModule(
    private val config: AdminComponentConfig = AdminComponentConfig(),
) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(AdminComponentConfig::class.java).toInstance(config)
            bind(AdminModulesProvider::class.java).toProvider(AdminModulesProviderProvider::class.java)
            bind(AdminComponent::class.java).asEagerSingleton()
        }
    }
}
