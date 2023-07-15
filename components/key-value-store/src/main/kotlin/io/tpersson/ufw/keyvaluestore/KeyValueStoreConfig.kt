package io.tpersson.ufw.keyvaluestore

import java.time.Duration

public data class KeyValueStoreConfig(
    val expiredEntryReapingInterval: Duration = Duration.ofSeconds(30)
) {
    public companion object {
        public val default: KeyValueStoreConfig = KeyValueStoreConfig()
    }
}