package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durableevents.common.DurableEventId
import io.tpersson.ufw.durableevents.publisher.DurableEventPublisher
import io.tpersson.ufw.examples.common.events.ExampleEventV1
import io.tpersson.ufw.examples.common.featuretoggles.AppFeatureToggles
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Clock
import java.time.Duration

public class PeriodicEventPublisher @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val transactionalEventPublisher: DurableEventPublisher,
    private val featureToggles: FeatureToggles,
    private val clock: Clock
) : ManagedPeriodicTask(
    interval = Duration.ofSeconds(5)
) {

    private val featureToggleHandle = featureToggles.get(AppFeatureToggles.PeriodicEventPublisher)

    private var i = 0

    override suspend fun runOnce() {
        i++

        if (!featureToggleHandle.isEnabled()) {
            return
        }

        unitOfWorkFactory.use { uow ->
            val event = ExampleEventV1(
                id = DurableEventId(),
                timestamp = clock.instant(),
                myContent = "$i"
            )
            transactionalEventPublisher.publish(event, uow)
        }
    }
}