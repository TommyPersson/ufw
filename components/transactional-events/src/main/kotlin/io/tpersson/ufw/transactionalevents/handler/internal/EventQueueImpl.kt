package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.core.concurrency.ConsumerSignal
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.TransactionalEventsConfig
import io.tpersson.ufw.transactionalevents.handler.EventQueueId
import io.tpersson.ufw.transactionalevents.handler.EventState
import io.tpersson.ufw.transactionalevents.handler.IncomingEvent
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventEntityData
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventFailuresDAO
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import io.tpersson.ufw.transactionalevents.handler.internal.metrics.EventQueueStatistics
import kotlinx.coroutines.time.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.util.*

public class EventQueueImpl(
    private val queueId: EventQueueId,
    private val queueDAO: EventQueueDAO,
    private val failuresDAO: EventFailuresDAO,
    private val clock: InstantSource,
    private val config: TransactionalEventsConfig,
) : EventQueue {

    private val pollWaitTime = config.queuePollWaitTime
    private val signal = ConsumerSignal()

    override val id: EventQueueId = queueId

    override suspend fun enqueue(event: IncomingEvent, unitOfWork: UnitOfWork) {
        val eventEntity = createEventEntity(event, clock.instant())

        queueDAO.insert(eventEntity, unitOfWork)

        unitOfWork.addPostCommitHook {
            signal.signal()
        }
    }

    override suspend fun pollOne(timeout: Duration): EventEntityData? {
        return withTimeoutOrNull(timeout) {
            var next = queueDAO.getNext(queueId, clock.instant())
            while (next == null) {
                signal.wait(pollWaitTime)
                next = queueDAO.getNext(queueId, clock.instant())
            }

            next
        }
    }

    override suspend fun markAsInProgress(eventId: EventId, watchdogId: String, unitOfWork: UnitOfWork) {
        queueDAO.markAsInProgress(
            queueId = queueId,
            eventId = eventId,
            now = clock.instant(),
            watchdogId = watchdogId,
            unitOfWork = unitOfWork
        )
    }

    override suspend fun updateWatchdog(eventUid: Long, watchdogId: String): Boolean {
        return queueDAO.updateWatchdog(
            eventUid = eventUid,
            now = clock.instant(),
            watchdogId = watchdogId
        )
    }

    override suspend fun recordFailure(eventUid: Long, error: Throwable, unitOfWork: UnitOfWork) {
        val failure = EventFailure(
            id = UUID.randomUUID(),
            eventUid = eventUid,
            timestamp = clock.instant(),
            errorType = error::class.simpleName!!,
            errorMessage = error.message ?: "<no message>",
            errorStackTrace = error.stackTraceToString()
        )

        failuresDAO.insert(failure, unitOfWork)
    }

    override suspend fun rescheduleAt(eventId: EventId, at: Instant, watchdogId: String, unitOfWork: UnitOfWork) {
        queueDAO.markAsScheduled(queueId, eventId, clock.instant(), at, watchdogId, unitOfWork)
    }

    override suspend fun getNumberOfFailuresFor(eventUid: Long): Int {
        return failuresDAO.getNumberOfFailuresFor(eventUid)
    }

    override suspend fun getStatistics(): EventQueueStatistics {
        return queueDAO.getQueueStatistics(queueId)
    }

    override suspend fun markAsSuccessful(eventId: EventId, watchdogId: String, unitOfWork: UnitOfWork) {
        val expireAt = clock.instant().plus(config.successfulEventRetention)

        queueDAO.markAsSuccessful(
            queueId = queueId,
            eventId = eventId,
            now = clock.instant(), expireAt = expireAt,
            watchdogId = watchdogId,
            unitOfWork = unitOfWork
        )
    }

    override suspend fun markAsFailed(eventId: EventId, watchdogId: String, unitOfWork: UnitOfWork) {
        val expireAt = clock.instant().plus(config.failedEventRetention)

        queueDAO.markAsFailed(queueId, eventId, clock.instant(), expireAt, watchdogId, unitOfWork)
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