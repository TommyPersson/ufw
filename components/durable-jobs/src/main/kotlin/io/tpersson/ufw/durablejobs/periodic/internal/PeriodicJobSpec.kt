package io.tpersson.ufw.durablejobs.periodic.internal

import com.cronutils.model.Cron
import io.tpersson.ufw.durablejobs.DurableJob
import io.tpersson.ufw.durablejobs.DurableJobHandler

public data class PeriodicJobSpec<T : DurableJob>(
    val handler: DurableJobHandler<T>,
    val cronExpression: String,
    val cronInstance: Cron,
)