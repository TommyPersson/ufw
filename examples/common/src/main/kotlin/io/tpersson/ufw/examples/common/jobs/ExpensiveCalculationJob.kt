package io.tpersson.ufw.examples.common.jobs

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.durablecaches.DurableCaches
import io.tpersson.ufw.durablejobs.*
import io.tpersson.ufw.examples.common.caches.AppCaches
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlin.random.Random


@DurableJobTypeDefinition(
    description = """
Performs "expensive" calculationsm
    """
)
public data class ExpensiveCalculationJob(
    override val id: DurableJobId = DurableJobId.new()
) : DurableJob

public class ExpensiveCalculationJobHandler @Inject constructor(
    private val caches: DurableCaches
) : DurableJobHandler<ExpensiveCalculationJob> {

    private val logger = createLogger()

    private val cache = caches.get(AppCaches.ExpensiveCalculations)

    override suspend fun handle(job: ExpensiveCalculationJob, context: DurableJobContext) {
        val calculationInput = Random.nextInt(0, 100)

        val result = cache.getOrPut(calculationInput.toString()) {
            logger.info("Performing expensive calculation...")
            delay(2_000)
            val result = calculationInput.toLong() * 2
            logger.info("Done!")
            result
        }.value

        logger.info("Calculated: $result")
    }
}