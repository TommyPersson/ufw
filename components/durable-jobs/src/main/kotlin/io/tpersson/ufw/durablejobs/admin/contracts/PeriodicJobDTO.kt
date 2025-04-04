package io.tpersson.ufw.durablejobs.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import io.tpersson.ufw.databasequeue.WorkQueueState
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import java.time.Instant

public data class PeriodicJobDTO(
    val type: String,
    val description: String?,
    val cronExpression: String,
    val cronDescription: String,
    val lastSchedulingAttempt: Instant?,
    val nextSchedulingAttempt: Instant?,
    val queueId: DurableJobQueueId,
    val queueState: WorkQueueState,
    val queueHasFailures: Boolean,
    val applicationModule: ApplicationModuleDTO,
)