package io.tpersson.ufw.examples.common.jobs

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.jobqueue.v2.*
import jakarta.inject.Inject
import org.slf4j.MDC

public data class PrintJob2(
    val text: String,
    override val id: JobId = JobId.new()
) : DurableJob

public class PrintJob2Handler @Inject constructor() : DurableJobHandler<PrintJob2> {

    private val logger = createLogger()

    override suspend fun handle(job: PrintJob2, context: JobContext) {
        println(MDC.getCopyOfContextMap())

        if (job.text.contains("2")) {
            error("oh no, there was a 2!")
        }

        logger.info("Handling: $job")
    }

    override suspend fun onFailure(job: PrintJob2, error: Exception, context: JobFailureContext): FailureAction {
        logger.error(":( Error during the job: $job (${context.failureCount})", error)

        if (context.failureCount > 5) {
            return FailureAction.GiveUp
        }

        return FailureAction.RescheduleNow
    }
}