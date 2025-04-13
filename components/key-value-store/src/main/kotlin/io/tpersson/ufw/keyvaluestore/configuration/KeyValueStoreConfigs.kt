package io.tpersson.ufw.keyvaluestore.configuration

import io.tpersson.ufw.core.configuration.ConfigElement
import io.tpersson.ufw.core.configuration.Configs
import java.time.Duration

public object KeyValueStoreConfigs {
    public val ExpiredEntryReapingInterval: ConfigElement<Duration> = ConfigElement.of(
        "key-value-store",
        "expired-entry-reaping-interval",
        default = Duration.ofSeconds(30)
    )
}

public val Configs.KeyValueStore: KeyValueStoreConfigs get() = KeyValueStoreConfigs