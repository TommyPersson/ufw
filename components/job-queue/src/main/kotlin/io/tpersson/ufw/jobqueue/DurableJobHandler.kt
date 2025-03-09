package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.databasequeue.FailureAction

public interface DurableJobHandler<TJob : DurableJob> {
    public suspend fun handle(job: TJob, context: JobContext)

    public suspend fun onFailure(job: TJob, error: Exception, context: JobFailureContext): FailureAction
}

