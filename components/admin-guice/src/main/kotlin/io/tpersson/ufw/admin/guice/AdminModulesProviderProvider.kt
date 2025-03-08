package io.tpersson.ufw.admin.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.internal.AdminModulesProvider
import io.tpersson.ufw.admin.internal.CoreAdminModule
import io.tpersson.ufw.admin.internal.SimpleAdminModulesProvider
import io.tpersson.ufw.core.NamedBindings
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Provider
import jakarta.inject.Singleton

@Singleton
public class AdminModulesProviderProvider @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : Provider<AdminModulesProvider> {

    private val modulesProvider = run {
        val adminModuleInstances = scanResult.allClasses
            .filter { it.implementsInterface(AdminModule::class.java) }
            .filter { !it.isAbstract }
            .filter { it.simpleName != CoreAdminModule::class.simpleName }
            .loadClasses()
            .map { injector.getInstance(it) as AdminModule }
            .toSet()

        SimpleAdminModulesProvider(adminModuleInstances).also {
            it.add(CoreAdminModule(it)) // clumsy way to avoid circular dependency
        }
    }

    override fun get(): AdminModulesProvider {
        return modulesProvider
    }
}