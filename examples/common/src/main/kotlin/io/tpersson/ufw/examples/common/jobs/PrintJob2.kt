package io.tpersson.ufw.examples.common.jobs

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.jobqueue.*
import io.tpersson.ufw.jobqueue.v2.DurableJob
import io.tpersson.ufw.jobqueue.v2.DurableJobHandler
import jakarta.inject.Inject

public data class PrintJob2(
    val text: String,
    override val id: String = JobId.new().value
) : DurableJob

public class PrintJob2Handler @Inject constructor() : DurableJobHandler<PrintJob2> {

    private val logger = createLogger()

    override suspend fun handle(job: PrintJob2) {
        logger.info("Handling: $job")
    }

    /*
    override suspend fun onFailure(job: PrintJob, error: Exception, context: JobFailureContext): FailureAction {
        return FailureAction.GiveUp
    }
     */
}