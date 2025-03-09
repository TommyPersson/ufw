package io.tpersson.ufw.jobqueue.v2.internal

import io.tpersson.ufw.jobqueue.v2.JobQueueId
import kotlin.reflect.KClass

public data class DurableJobDefinition<TJob : Any>(
    public val queueId: JobQueueId,
    public val type: String,
    public val jobClass: KClass<TJob>
)