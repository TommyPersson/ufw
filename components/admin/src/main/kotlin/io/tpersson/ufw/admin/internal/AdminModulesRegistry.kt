package io.tpersson.ufw.admin.internal

import io.tpersson.ufw.admin.AdminModule

public interface AdminModulesRegistry {
    public fun get(): List<AdminModule>
}