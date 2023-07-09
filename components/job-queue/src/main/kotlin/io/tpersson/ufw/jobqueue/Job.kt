package io.tpersson.ufw.jobqueue

public interface Job {
    public val jobId: JobId
}

public val <TJob : Job> TJob.queueId: JobQueueId<TJob> get() = JobQueueId(this::class)