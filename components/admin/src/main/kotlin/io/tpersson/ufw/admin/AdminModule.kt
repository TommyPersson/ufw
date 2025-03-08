package io.tpersson.ufw.admin

import io.ktor.server.application.*

public interface AdminModule {
    public fun configure(application: Application)
}