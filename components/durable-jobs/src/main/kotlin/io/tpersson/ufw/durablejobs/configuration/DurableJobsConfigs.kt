package io.tpersson.ufw.durablejobs.configuration

import io.tpersson.ufw.core.configuration.ConfigElement
import io.tpersson.ufw.core.configuration.Configs
import java.time.Duration

public object DurableJobsConfigs {
    public val PeriodicJobsCheckInterval: ConfigElement<Duration> = ConfigElement.of(
        "durable-jobs",
        "periodic-jobs-check-interval",
        default = Duration.ofSeconds(30)
    )
}

public val Configs.DurableJobs: DurableJobsConfigs get() = DurableJobsConfigs