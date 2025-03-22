package io.tpersson.ufw.durableevents

import java.time.Duration

public data class DurableEventsConfig(
    val queuePollWaitTime: Duration = Duration.ofSeconds(5),
    val watchdogRefreshInterval: Duration = Duration.ofSeconds(5),
    val stalenessDetectionInterval: Duration = Duration.ofMinutes(1),
    val stalenessAge: Duration = Duration.ofMinutes(10),
    val successfulEventRetention: Duration = Duration.ofDays(1),
    val failedEventRetention: Duration = Duration.ofDays(14),
    val expiredEventReapingInterval: Duration = Duration.ofMinutes(1),
    val metricMeasurementInterval: Duration = Duration.ofSeconds(30),
) {

    public companion object {
        public val default: DurableEventsConfig = DurableEventsConfig()
    }
}