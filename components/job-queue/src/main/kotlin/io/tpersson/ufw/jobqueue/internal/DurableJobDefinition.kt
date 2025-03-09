package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.JobQueueId
import kotlin.reflect.KClass

public data class DurableJobDefinition<TJob : Any>(
    public val queueId: JobQueueId,
    public val type: String,
    public val jobClass: KClass<TJob>,
    public val description: String?,
)