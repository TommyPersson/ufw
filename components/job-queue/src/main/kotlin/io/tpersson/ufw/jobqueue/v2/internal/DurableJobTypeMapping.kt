package io.tpersson.ufw.jobqueue.v2.internal

import io.tpersson.ufw.jobqueue.v2.DurableJobHandler

public data class DurableJobTypeMapping<TJob : Any>(
    val jobDefinition: DurableJobDefinition<TJob>,
    val handler: DurableJobHandler<out Any>
)