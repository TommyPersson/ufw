package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durableevents.common.DurableEventId
import io.tpersson.ufw.examples.common.events.ExampleEventV1
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.durableevents.publisher.DurableEventPublisher
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.InstantSource

public class PeriodicEventPublisher @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val transactionalEventPublisher: DurableEventPublisher,
    private val clock: InstantSource
) : ManagedJob() {

    override suspend fun launch(): Unit = coroutineScope {
        var i = 0

        forever(logger) {
            i++

            withContext(NonCancellable) {
                unitOfWorkFactory.use { uow ->
                    val event = ExampleEventV1(
                        id = DurableEventId(),
                        timestamp = clock.instant(),
                        myContent = "$i"
                    )
                    transactionalEventPublisher.publish(event, uow)
                }

                delay(5_000)
            }
        }
    }
}