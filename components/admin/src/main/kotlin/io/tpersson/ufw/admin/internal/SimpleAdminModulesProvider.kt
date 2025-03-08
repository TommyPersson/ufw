package io.tpersson.ufw.admin.internal

import io.tpersson.ufw.admin.AdminModule

public class SimpleAdminModulesProvider(
    modules: Set<AdminModule> = emptySet(),
) : AdminModulesProvider {

    private val _modules = modules.toMutableList()

    public fun add(module: AdminModule) {
        _modules.add(module)
    }

    override fun get(): List<AdminModule> {
        return _modules
    }
}