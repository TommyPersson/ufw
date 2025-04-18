package io.tpersson.ufw.cluster.configuration

import io.tpersson.ufw.core.configuration.ConfigElement
import io.tpersson.ufw.core.configuration.Configs
import java.time.Duration

public object ClusterConfigs {
    public val HeartbeatWorkerInterval: ConfigElement<Duration> = ConfigElement.of(
        "cluster",
        "heartbeat-worker-interval",
        default = Duration.ofSeconds(30)
    )

    public val InstanceStalenessAge: ConfigElement<Duration> = ConfigElement.of(
        "cluster",
        "instance-staleness-age",
        default = Duration.ofMinutes(2)
    )
}

public val Configs.Cluster: ClusterConfigs get() = ClusterConfigs