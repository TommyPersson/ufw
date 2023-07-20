package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.transactionalevents.handler.EventQueueId
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventFailuresDAO
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.InstantSource
import java.util.concurrent.ConcurrentHashMap

@Singleton
public class EventQueueProviderImpl @Inject constructor(
    private val eventQueueDAO: EventQueueDAO,
    private val eventFailuresDAO: EventFailuresDAO,
    private val clock: InstantSource
) : EventQueueProvider {

    private val queues = ConcurrentHashMap<EventQueueId, EventQueue>()
    private val queuesMutex = Mutex()

    override suspend fun get(queueId: EventQueueId): EventQueue {
        return queuesMutex.withLock {
            queues.getOrPut(queueId) {
                EventQueueImpl(
                    queueId = queueId,
                    queueDAO = eventQueueDAO,
                    failuresDAO = eventFailuresDAO,
                    clock = clock
                )
            }
        }
    }
}