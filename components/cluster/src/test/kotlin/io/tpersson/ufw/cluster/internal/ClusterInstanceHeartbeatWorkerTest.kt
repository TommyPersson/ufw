package io.tpersson.ufw.cluster.internal

import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.managed.ManagedJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

internal class ClusterInstanceHeartbeatWorkerTest {

    private lateinit var clusterInstancesService: ClusterInstancesService

    private lateinit var worker: ClusterInstanceHeartbeatWorker

    @BeforeEach
    fun setUp(){
        clusterInstancesService = mock<ClusterInstancesService>()

        worker = ClusterInstanceHeartbeatWorker(
            configProvider = ConfigProvider.empty(),
            clusterInstancesService = clusterInstancesService,
        )
    }

    @Test
    fun `runOnce - Shall record a heartbeat`(): Unit = runBlocking {
        worker.runOnce()

        verify(clusterInstancesService).recordHeartbeat()
    }

    @Test
    fun `onStopped - Shall remove the instance`(): Unit = runBlocking {
        worker.start()

        verifyNoInteractions(clusterInstancesService)

        worker.stop()

        verify(clusterInstancesService).removeInstance()
    }

}