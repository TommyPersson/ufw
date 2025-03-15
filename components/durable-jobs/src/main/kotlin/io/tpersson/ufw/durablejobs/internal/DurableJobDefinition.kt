package io.tpersson.ufw.durablejobs.internal

import io.tpersson.ufw.durablejobs.DurableJobQueueId
import kotlin.reflect.KClass

public data class DurableJobDefinition<TJob : Any>(
    public val queueId: DurableJobQueueId,
    public val type: String,
    public val jobClass: KClass<TJob>,
    public val description: String?,
)