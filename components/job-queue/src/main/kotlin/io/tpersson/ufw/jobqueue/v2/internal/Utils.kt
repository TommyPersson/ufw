package io.tpersson.ufw.jobqueue.v2.internal

import io.tpersson.ufw.jobqueue.v2.DurableJobHandler
import io.tpersson.ufw.jobqueue.v2.WithDurableJobDefinition
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

public val <TJob : Any> KClass<out DurableJobHandler<TJob>>.jobDefinition: DurableJobDefinition<TJob>
    get() {
        val jobClass = this.supertypes
            .first { it.classifier == DurableJobHandler::class }
            .arguments[0]
            .type
            ?.classifier as? KClass<TJob>
            ?: error("Unable to determine job type for handler $this")

        val annotation = jobClass.findAnnotation<WithDurableJobDefinition>()

        return DurableJobDefinition(
            queueId = annotation?.queueId ?: jobClass.simpleName!!,
            type = annotation?.type ?: jobClass.simpleName!!,
            jobClass = jobClass,
        )
    }


public val <TJob : Any> DurableJobHandler<TJob>.jobDefinition: DurableJobDefinition<TJob>
    get() = this::class.jobDefinition