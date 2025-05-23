package io.tpersson.ufw.examples.common.jobs

import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.durablejobs.*
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommand
import io.tpersson.ufw.mediator.Mediator
import jakarta.inject.Inject
import org.slf4j.MDC
import java.time.Duration
import kotlin.random.Random

@DurableJobTypeDefinition(
    description = """
This is a very **cool** job that performs a greeting command! Though, it is unable to greet texts containing unlucky numbers.
    """
)
public data class PrintJob2(
    val text: String,
    override val id: DurableJobId = DurableJobId.new()
) : DurableJob

public class PrintJob2Handler @Inject constructor(
    private val mediator: Mediator
) : DurableJobHandler<PrintJob2> {

    override suspend fun handle(job: PrintJob2, context: DurableJobContext) {
        context.logger.info(MDC.getCopyOfContextMap().toString())

        val unluckyNumber = Random.nextInt(0, 10)

        context.logger.info("Todays unlucky number: $unluckyNumber")

        val isUnlucky = job.text.contains(unluckyNumber.toString())

        if (isUnlucky) {
            error("oh no, there was a unlucky ${unluckyNumber}!")
        }

        mediator.send(PerformGreetingCommand(target = job.text))
    }

    override suspend fun onFailure(job: PrintJob2, error: Exception, context: DurableJobFailureContext): FailureAction {
        context.logger.error(":( Error during the job: $job (${context.failureCount})", error)

        if (context.failureCount > 2) {
            return FailureAction.GiveUp
        }

        return FailureAction.RescheduleAt(context.timestamp + Duration.ofSeconds(60))
    }
}