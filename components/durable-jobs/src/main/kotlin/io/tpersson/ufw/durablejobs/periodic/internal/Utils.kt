package io.tpersson.ufw.durablejobs.periodic.internal

import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobStateData


public data class SpecKey(val queueId: String, val jobType: String)

public val PeriodicJobStateData.key: SpecKey get() = SpecKey(queueId = queueId, jobType = jobType)

public val PeriodicJobSpec<*>.key: SpecKey
    get() = SpecKey(
        queueId = handler.jobDefinition.queueId.value,
        jobType = handler.jobDefinition.type
    )