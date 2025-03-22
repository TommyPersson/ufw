package io.tpersson.ufw.durablejobs

import java.time.Duration

public data class DurableJobsConfig( // TODO actually pass these to the unerlying database queue component
    val stalenessDetectionInterval: Duration = Duration.ofMinutes(1),
    val stalenessAge: Duration = Duration.ofMinutes(10),
    val watchdogRefreshInterval: Duration = Duration.ofSeconds(5),
    val pollWaitTime: Duration = Duration.ofSeconds(5),
    val defaultJobTimeout: Duration = Duration.ofMinutes(10),
    val successfulJobRetention: Duration =  Duration.ofDays(1),
    val failedJobRetention: Duration = Duration.ofDays(14),
    val expiredJobReapingInterval: Duration = Duration.ofMinutes(1),
    val metricMeasurementInterval: Duration = Duration.ofSeconds(30),
) {

    public companion object {
        public val Default: DurableJobsConfig = DurableJobsConfig()
    }
}