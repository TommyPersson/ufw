package io.tpersson.ufw.admin.internal

import io.tpersson.ufw.admin.AdminModule

public class SimpleAdminModulesRegistry(
    modules: Set<AdminModule> = emptySet(),
) : AdminModulesRegistry {

    private val _modules = modules.toMutableList()

    public fun add(module: AdminModule) {
        _modules.add(module)
    }

    override fun get(): List<AdminModule> {
        return _modules
    }
}