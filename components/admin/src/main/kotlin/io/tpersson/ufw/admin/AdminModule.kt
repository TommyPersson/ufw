package io.tpersson.ufw.admin

import io.ktor.server.application.*

public interface AdminModule {
    public val moduleId: String

    public fun configure(application: Application)
}