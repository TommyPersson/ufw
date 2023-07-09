package io.tpersson.ufw.jobqueue

import kotlin.reflect.KClass

public data class JobQueueId<TJob : Job>(public val jobType: KClass<out TJob>) {
    public val typeName: String get() = jobType.simpleName!!
}