package io.tpersson.ufw.admin.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.AdminComponentConfig

public class AdminGuiceModule(
    private val config: AdminComponentConfig = AdminComponentConfig(),
) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(AdminComponentConfig::class.java).toInstance(config)
            bind(AdminComponent::class.java).asEagerSingleton()
        }
    }
}