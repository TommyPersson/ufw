package io.tpersson.ufw.jobqueue.v2.internal

import kotlin.reflect.KClass

public data class DurableJobDefinition<TJob : Any>(
    public val queueId: String,
    public val type: String,
    public val jobClass: KClass<TJob>
)