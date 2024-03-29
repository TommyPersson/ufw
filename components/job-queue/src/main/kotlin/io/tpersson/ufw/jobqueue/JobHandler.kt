package io.tpersson.ufw.jobqueue

import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes

public abstract class JobHandler<TJob : Job> {
    public abstract suspend fun handle(job: TJob, context: JobContext): Unit

    public abstract suspend fun onFailure(job: TJob, error: Exception, context: JobFailureContext): FailureAction

    public val jobType: KClass<TJob> = javaClass.kotlin
        .allSupertypes.first { it.classifier == JobHandler::class }
        .arguments[0]
        .type!!
        .classifier as KClass<TJob>

    public val queueId: JobQueueId<TJob> = JobQueueId(jobType)
}

