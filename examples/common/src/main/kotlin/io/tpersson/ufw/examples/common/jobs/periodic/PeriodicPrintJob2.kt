package io.tpersson.ufw.examples.common.jobs.periodic

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.durablejobs.*
import jakarta.inject.Inject
import kotlin.random.Random

@DurableJobTypeDefinition(
    description = """
It also prints stuff regularly.
"""
)
@PeriodicJob("*/2 * * * *")
public data class PeriodicPrintJob2(
    val text: String = "Another Thing To Print",
    override val id: DurableJobId = DurableJobId.new()
) : DurableJob

public class PeriodicPrintJob2Handler @Inject constructor() : DurableJobHandler<PeriodicPrintJob2> {

    private val logger = createLogger()

    override suspend fun handle(job: PeriodicPrintJob2, context: DurableJobContext) {
        logger.info("Handling: $job")
    }

    override suspend fun onFailure(
        job: PeriodicPrintJob2,
        error: Exception,
        context: DurableJobFailureContext
    ): FailureAction {
        return FailureAction.GiveUp
    }
}