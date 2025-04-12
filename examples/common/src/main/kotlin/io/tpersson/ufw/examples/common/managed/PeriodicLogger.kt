package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.examples.common.featuretoggles.AppFeatureToggles
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Duration

public class PeriodicLogger @Inject constructor(
    private val featureToggles: FeatureToggles
) : ManagedPeriodicTask(
    interval = Duration.ofSeconds(1)
) {

    private val featureToggle = featureToggles.get(AppFeatureToggles.PeriodicLogger)

    override suspend fun runOnce() {
        if (featureToggle.isEnabled()) {
            logger.info("Heartbeat")
        }
    }
}