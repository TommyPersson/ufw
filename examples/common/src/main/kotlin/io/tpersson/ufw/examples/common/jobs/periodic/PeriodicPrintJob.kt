package io.tpersson.ufw.examples.common.jobs.periodic

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.durablejobs.*
import io.tpersson.ufw.durablejobs.periodic.PeriodicJob
import jakarta.inject.Inject
import kotlin.random.Random

@DurableJobTypeDefinition(
    description = """
It prints stuff regularly. 

Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod.
"""
)
@PeriodicJob("* * * * *")
public data class PeriodicPrintJob(
    val text: String = "Thing To Print",
    override val id: DurableJobId = DurableJobId.new()
) : DurableJob

public class PeriodicPrintJobHandler @Inject constructor() : DurableJobHandler<PeriodicPrintJob> {

    private val logger = createLogger()

    override suspend fun handle(job: PeriodicPrintJob, context: DurableJobContext) {
        if (Random.nextInt() % 2 == 0) {
            error("Uh oh")
        }

        logger.info("Handling: $job")
    }

    override suspend fun onFailure(
        job: PeriodicPrintJob,
        error: Exception,
        context: DurableJobFailureContext
    ): FailureAction {
        return FailureAction.GiveUp
    }
}