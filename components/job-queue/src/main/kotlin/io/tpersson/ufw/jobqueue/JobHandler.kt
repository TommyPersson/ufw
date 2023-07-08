package io.tpersson.ufw.jobqueue

import java.time.Instant

public interface JobHandler<TJob : Job> {
    public suspend fun handle(job: TJob, context: JobContext): Unit

    public suspend fun onFailure(job: TJob, error: Exception, context: JobFailureContext): FailureAction
}

public sealed class FailureAction {
    public class Reschedule(public val at: Instant) : FailureAction()
    public object GiveUp : FailureAction()
}