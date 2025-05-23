package io.tpersson.ufw.databasequeue.worker

import com.github.benmanes.caffeine.cache.Caffeine
import io.tpersson.ufw.core.utils.ClockTicker
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkQueueState
import io.tpersson.ufw.databasequeue.internal.WorkQueuesDAO
import jakarta.inject.Inject
import java.time.Duration
import java.time.Clock

public class CachingQueueStateCheckerImpl @Inject constructor(
    private val workQueuesDAO: WorkQueuesDAO,
    private val clock: Clock
) : QueueStateChecker {

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(5))
        .ticker(ClockTicker(clock))
        .build<WorkItemQueueId, Boolean>()

    override suspend fun isQueuePaused(queueId: WorkItemQueueId): Boolean {
        val cached = cache.getIfPresent(queueId)
        if (cached != null) {
            return cached
        }

        val isPaused = workQueuesDAO.getWorkQueue(queueId)?.state == WorkQueueState.PAUSED.name

        cache.put(queueId, isPaused)

        return isPaused
    }

}