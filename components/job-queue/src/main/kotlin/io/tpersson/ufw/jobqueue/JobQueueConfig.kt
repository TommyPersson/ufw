package io.tpersson.ufw.jobqueue

import java.time.Duration

public data class JobQueueConfig(
    val stalenessDetectionInterval: Duration = Duration.ofMinutes(1),
    val stalenessAge: Duration = Duration.ofMinutes(1),
    val watchdogRefreshInterval: Duration = Duration.ofSeconds(5),
    val pollWaitTime: Duration = Duration.ofSeconds(5),
    val defaultJobTimeout: Duration = Duration.ofMinutes(10),
    val defaultJobRetention: Duration =  Duration.ofDays(14),
) {
    public companion object {
        public val Default: JobQueueConfig = JobQueueConfig()
    }
}