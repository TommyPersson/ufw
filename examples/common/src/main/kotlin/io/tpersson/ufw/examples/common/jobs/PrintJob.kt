package io.tpersson.ufw.examples.common.jobs

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.jobqueue.*
import jakarta.inject.Inject

public data class PrintJob(
    val text: String,
    override val id: JobId = JobId.new()
) : DurableJob

public class PrintJobHandler @Inject constructor() : DurableJobHandler<PrintJob> {

    private val logger = createLogger()

    override suspend fun handle(job: PrintJob, context: JobContext) {
        logger.info("Handling: $job")
    }

    override suspend fun onFailure(job: PrintJob, error: Exception, context: JobFailureContext): FailureAction {
        return FailureAction.GiveUp
    }
}