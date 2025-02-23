package io.tpersson.ufw.jobqueue.v2

import io.tpersson.ufw.databasequeue.FailureAction

public interface DurableJobHandler<TJob : Any> {
    public suspend fun handle(job: TJob)

    public suspend fun onFailure(job: TJob, error: Exception, context: JobFailureContext): FailureAction
}

