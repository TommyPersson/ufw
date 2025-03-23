package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.examples.common.featuretoggles.AppFeatureToggles
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.*

public class PeriodicLogger @Inject constructor(
    private val featureToggles: FeatureToggles
) : ManagedJob() {

    private val featureToggle = featureToggles.get(AppFeatureToggles.PeriodicLogger)

    override suspend fun launch(): Unit = coroutineScope {
        while (isActive) {
            withContext(NonCancellable) {
                delay(1000)

                if (featureToggle.isEnabled()) {
                    logger.info("Heartbeat")
                }
            }
        }

        logger.info("Stopping")
    }
}