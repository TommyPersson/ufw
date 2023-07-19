package io.tpersson.ufw.transactionalevents

import java.time.Duration

public data class TransactionalEventsConfig(
    val thing: Boolean = true,
    val watchdogRefreshInterval: Duration = Duration.ofSeconds(5)
) {
    public companion object {
        public val default: TransactionalEventsConfig = TransactionalEventsConfig()
    }
}