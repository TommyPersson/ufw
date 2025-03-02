package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.managed.ManagedJob
import kotlinx.coroutines.*

public class PeriodicLogger : ManagedJob() {

    override suspend fun launch(): Unit = coroutineScope {
        while (isActive) {
            withContext(NonCancellable) {
                logger.info("Heartbeat")
                delay(1000)
            }
        }

        logger.info("Stopping")
    }
}