package io.tpersson.ufw.transactionalevents

import java.time.Duration

public data class TransactionalEventsConfig(
    val queuePollWaitTime: Duration = Duration.ofSeconds(5),
    val watchdogRefreshInterval: Duration = Duration.ofSeconds(5),
    val stalenessDetectionInterval: Duration = Duration.ofMinutes(1),
    val stalenessAge: Duration = Duration.ofMinutes(10),
    val successfulEventRetention: Duration = Duration.ofDays(1),
    val failedEventRetention: Duration = Duration.ofDays(14),
    val expiredEventReapingInterval: Duration = Duration.ofMinutes(1)
) {

    public companion object {
        public val default: TransactionalEventsConfig = TransactionalEventsConfig()
    }
}