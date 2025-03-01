package io.tpersson.ufw.jobqueue.v2.internal

import io.tpersson.ufw.jobqueue.v2.DurableJob
import io.tpersson.ufw.jobqueue.v2.DurableJobHandler
import io.tpersson.ufw.jobqueue.v2.WithDurableJobDefinition
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

public val <TJob : DurableJob> KClass<out DurableJobHandler<TJob>>.jobDefinition: DurableJobDefinition<TJob>
    get() {
        val jobClass = this.supertypes
            .first { it.classifier == DurableJobHandler::class }
            .arguments[0]
            .type
            ?.classifier as? KClass<TJob>
            ?: error("Unable to determine job type for handler $this")

        return jobClass.jobDefinition2
    }


public val <TJob : DurableJob> DurableJobHandler<TJob>.jobDefinition: DurableJobDefinition<TJob>
    get() = this::class.jobDefinition

// TODO better names for JVM disambiguation
public val <TJob : DurableJob> KClass<TJob>.jobDefinition2: DurableJobDefinition<TJob>
    get() = findAnnotation<WithDurableJobDefinition>().let { annotation ->
        DurableJobDefinition(
            queueId = annotation?.queueId ?: simpleName!!,
            type = annotation?.type ?: simpleName!!,
            jobClass = this,
        )
    }