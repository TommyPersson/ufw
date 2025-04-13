package io.tpersson.ufw.admin.configuration

import io.tpersson.ufw.core.configuration.ConfigElement
import io.tpersson.ufw.core.configuration.Configs

public object AdminConfigs {
    public val ServerPort: ConfigElement<Int> = ConfigElement.of(
        "admin",
        "server-port",
        default = 8081
    )
}

@Suppress("UnusedReceiverParameter")
public val Configs.Admin: AdminConfigs get() = AdminConfigs
