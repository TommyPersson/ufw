package io.tpersson.ufw.durablejobs

import io.tpersson.ufw.databasequeue.FailureAction

public interface DurableJobHandler<TJob : DurableJob> {
    public suspend fun handle(job: TJob, context: DurableJobContext)

    public suspend fun onFailure(job: TJob, error: Exception, context: DurableJobFailureContext): FailureAction =
        FailureAction.GiveUp
}

