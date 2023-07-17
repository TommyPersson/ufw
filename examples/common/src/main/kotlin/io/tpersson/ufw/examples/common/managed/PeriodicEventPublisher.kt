package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.examples.common.events.ExampleEventV1
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.publisher.TransactionalEventPublisher
import jakarta.inject.Inject
import kotlinx.coroutines.*
import java.time.InstantSource

public class PeriodicEventPublisher @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val transactionalEventPublisher: TransactionalEventPublisher,
    private val clock: InstantSource
) : ManagedJob() {
    private val logger = createLogger()

    override suspend fun launch(): Unit = coroutineScope {
        var i = 0

        forever(logger) {
            i++

            withContext(NonCancellable) {
                unitOfWorkFactory.use { uow ->
                    val event = ExampleEventV1(
                        id = EventId(),
                        timestamp = clock.instant(),
                        myContent = "$i"
                    )
                    transactionalEventPublisher.publish("example-topic", event, uow)
                }

                delay(5_000)
            }
        }
    }
}