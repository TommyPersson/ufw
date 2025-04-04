package io.tpersson.ufw.examples.common.jobs.periodic

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.durablejobs.*
import jakarta.inject.Inject

@DurableJobTypeDefinition(
    description = "It prints stuff regularly"
)
@PeriodicJob("5-59/10 * * * *") // Every 10 minutes, minutes 5 through 59 past the hour
public data class PeriodicPrintJob(
    val text: String = "Thing To Print",
    override val id: DurableJobId = DurableJobId.new()
) : DurableJob

public class PeriodicPrintJobHandler @Inject constructor() : DurableJobHandler<PeriodicPrintJob> {

    private val logger = createLogger()

    override suspend fun handle(job: PeriodicPrintJob, context: DurableJobContext) {
        logger.info("Handling: $job")
    }

    override suspend fun onFailure(job: PeriodicPrintJob, error: Exception, context: DurableJobFailureContext): FailureAction {
        return FailureAction.GiveUp
    }
}