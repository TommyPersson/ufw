package io.tpersson.ufw.jobqueue

import java.time.Instant
import kotlin.reflect.KClass

public abstract class JobHandler<TJob : Job> {
    public abstract suspend fun handle(job: TJob, context: JobContext): Unit

    public abstract suspend fun onFailure(job: TJob, error: Exception, context: JobFailureContext): FailureAction

    public val jobType: KClass<TJob> = javaClass.kotlin
        .supertypes[0]
        .arguments[0]
        .type!!
        .classifier as KClass<TJob>

    public val queueId: JobQueueId<TJob> = JobQueueId(jobType)
}

public sealed class FailureAction {
    public class Reschedule(public val at: Instant) : FailureAction()
    public object RescheduleNow : FailureAction()
    public object GiveUp : FailureAction()
}