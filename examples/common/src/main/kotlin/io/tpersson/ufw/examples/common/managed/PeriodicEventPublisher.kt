package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.publisher.DurableMessagePublisher
import io.tpersson.ufw.examples.common.messages.ExampleEventV1
import io.tpersson.ufw.examples.common.featuretoggles.AppFeatureToggles
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Clock
import java.time.Duration

public class PeriodicEventPublisher @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val messagePublisher: DurableMessagePublisher,
    private val featureToggles: FeatureToggles,
    private val clock: Clock
) : ManagedPeriodicTask(
    interval = Duration.ofMillis(10)
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
                id = DurableMessageId(),
                timestamp = clock.instant(),
                myContent = "$i"
            )
            messagePublisher.publish(event, uow)
        }
    }
}