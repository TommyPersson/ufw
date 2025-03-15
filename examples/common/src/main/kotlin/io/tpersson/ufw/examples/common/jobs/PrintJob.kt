package io.tpersson.ufw.examples.common.jobs

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.durablejobs.*
import jakarta.inject.Inject

public data class PrintJob(
    val text: String,
    override val id: DurableJobId = DurableJobId.new()
) : DurableJob

public class PrintJobHandler @Inject constructor() : DurableJobHandler<PrintJob> {

    private val logger = createLogger()

    override suspend fun handle(job: PrintJob, context: DurableJobContext) {
        logger.info("Handling: $job")
    }

    override suspend fun onFailure(job: PrintJob, error: Exception, context: DurableJobFailureContext): FailureAction {
        return FailureAction.GiveUp
    }
}