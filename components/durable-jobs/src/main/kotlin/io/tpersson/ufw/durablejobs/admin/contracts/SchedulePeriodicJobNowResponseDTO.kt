package io.tpersson.ufw.durablejobs.admin.contracts

import io.tpersson.ufw.durablejobs.DurableJobId

public data class SchedulePeriodicJobNowResponseDTO(
    val jobId: DurableJobId
)