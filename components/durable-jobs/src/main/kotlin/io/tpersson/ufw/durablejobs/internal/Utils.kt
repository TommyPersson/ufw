package io.tpersson.ufw.durablejobs.internal

import io.tpersson.ufw.databasequeue.WorkItemId
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.convertQueueId
import io.tpersson.ufw.durablejobs.*
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
    get() = findAnnotation<DurableJobTypeDefinition>().let { annotation ->
        DurableJobDefinition(
            queueId = DurableJobQueueId.fromString(annotation?.queueId.ifNullOrBlank { simpleName!!}),
            type = annotation?.type.ifNullOrBlank { simpleName!! },
            jobClass = this,
            description = annotation?.description?.trim(),
        )
    }


public fun DurableJobQueueId.toWorkItemQueueId(): WorkItemQueueId {
    return DurableJobsDatabaseQueueAdapterSettings.convertQueueId(this.value)
}

public fun DurableJobId.toWorkItemId(): WorkItemId {
    return WorkItemId(this.value)
}

public fun String?.ifNullOrBlank(default: () -> String): String {
    return if (this.isNullOrBlank()) default() else this
}