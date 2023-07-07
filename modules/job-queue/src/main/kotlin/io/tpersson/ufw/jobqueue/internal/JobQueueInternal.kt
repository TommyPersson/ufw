package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobQueue
import io.tpersson.ufw.jobqueue.JobQueueId
import java.time.Duration

public interface JobQueueInternal : JobQueue {
    public suspend fun <TJob : Job> pollOne(queueId: JobQueueId<TJob>, timeout: Duration): InternalJob<TJob>?
}