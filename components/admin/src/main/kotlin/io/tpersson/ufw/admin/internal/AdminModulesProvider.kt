package io.tpersson.ufw.admin.internal

import io.tpersson.ufw.admin.AdminModule

public interface AdminModulesProvider {
    public fun get(): List<AdminModule>
}