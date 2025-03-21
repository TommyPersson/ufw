package io.tpersson.ufw.examples.common.jobs

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.durablejobs.*
import jakarta.inject.Inject
import org.slf4j.MDC
import java.time.Duration
import kotlin.random.Random

@DurableJobTypeDefinition(
    description = """
This is a very **cool** job that prints *things*!
    """
)
public data class PrintJob2(
    val text: String,
    override val id: DurableJobId = DurableJobId.new()
) : DurableJob

public class PrintJob2Handler @Inject constructor() : DurableJobHandler<PrintJob2> {

    private val logger = createLogger()

    override suspend fun handle(job: PrintJob2, context: DurableJobContext) {
        println(MDC.getCopyOfContextMap())

        val unluckyNumber = Random.nextInt(0, 10)

        logger.info("Todays unlucky number: $unluckyNumber")

        if (job.text.contains(unluckyNumber.toString())) {
            error("oh no, there was a unlucky ${unluckyNumber}!")
        }

        logger.info("Handling: $job")
    }

    override suspend fun onFailure(job: PrintJob2, error: Exception, context: DurableJobFailureContext): FailureAction {
        logger.error(":( Error during the job: $job (${context.failureCount})", error)

        if (context.failureCount > 2) {
            return FailureAction.GiveUp
        }

        return FailureAction.RescheduleAt(context.timestamp + Duration.ofSeconds(60))
    }
}