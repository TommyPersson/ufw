package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.core.concurrency.ConsumerSignal
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.handler.EventQueueId
import io.tpersson.ufw.transactionalevents.handler.EventState
import io.tpersson.ufw.transactionalevents.handler.IncomingEvent
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventEntityData
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import kotlinx.coroutines.time.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

public class EventQueueImpl(
    private val queueId: EventQueueId,
    private val dao: EventQueueDAO,
    private val clock: InstantSource,
) : EventQueue {

    private val pollWaitTime = Duration.ofSeconds(5)
    private val signal = ConsumerSignal()

    override suspend fun enqueue(event: IncomingEvent, unitOfWork: UnitOfWork) {
        val eventEntity = createEventEntity(event, clock.instant())

        dao.insert(eventEntity, unitOfWork)

        unitOfWork.addPostCommitHook {
            signal.signal()
        }
    }

    override suspend fun pollOne(timeout: Duration): EventEntityData? {
        return withTimeoutOrNull(timeout) {
            var next = dao.getNext(queueId, clock.instant())
            while (next == null) {
                signal.wait(pollWaitTime)
                next = dao.getNext(queueId, clock.instant())
            }

            next
        }
    }

    override suspend fun markAsInProgress(id: EventId, watchdogId: String, uow: UnitOfWork) {
        dao.markAsInProgress(
            eventQueueId = queueId,
            eventId = id,
            now = clock.instant(),
            watchdogId = watchdogId,
            unitOfWork = uow
        )
    }

    override suspend fun updateWatchdog(id: EventId, watchdogId: String): Boolean {
        return true
        //TODO("Not yet implemented")
    }

    override suspend fun markAsSuccessful(id: EventId, watchdogId: String, uow: UnitOfWork) {
        val expireAt = Instant.now().plus(Duration.ofDays(1)) // TODO config

        dao.markAsSuccessful(
            eventQueueId = queueId,
            eventId = id,
            now = clock.instant(), expireAt = expireAt,
            watchdogId = watchdogId,
            unitOfWork = uow
        )
    }

    private fun createEventEntity(
        event: IncomingEvent,
        now: Instant
    ): EventEntityData {
        return EventEntityData(
            queueId = queueId.id,
            id = event.id.value.toString(),
            topic = event.topic,
            type = event.type,
            dataJson = event.dataJson,
            ceDataJson = "{}",
            timestamp = event.timestamp,
            state = EventState.Scheduled.id,
            createdAt = now,
            scheduledFor = now,
            stateChangedAt = now,
            watchdogTimestamp = null,
            watchdogOwner = null,
            expireAt = null,
        )
    }
}