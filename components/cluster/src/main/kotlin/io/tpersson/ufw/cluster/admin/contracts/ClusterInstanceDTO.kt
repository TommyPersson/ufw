package io.tpersson.ufw.cluster.admin.contracts

import java.time.Instant

public data class ClusterInstanceDTO(
    val instanceId: String,
    val appVersion: String,
    val startedAt: Instant,
    val heartbeatTimestamp: Instant,
)