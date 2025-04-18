package io.tpersson.ufw.cluster.internal

import io.tpersson.ufw.cluster.configuration.Cluster
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject

public class ClusterInstanceHeartbeatWorker @Inject constructor(
    private val configProvider: ConfigProvider,
    private val clusterInstancesService: ClusterInstancesService
) : ManagedPeriodicTask(
    interval = configProvider.get(Configs.Cluster.HeartbeatWorkerInterval),
    waitFirst = false
) {
    override suspend fun runOnce() {
        clusterInstancesService.recordHeartbeat()
    }

    override suspend fun onStopped() {
        super.onStopped()

        clusterInstancesService.removeInstance()
    }
}