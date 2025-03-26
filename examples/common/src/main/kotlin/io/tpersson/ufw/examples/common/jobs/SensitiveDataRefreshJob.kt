package io.tpersson.ufw.examples.common.jobs

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.durablecaches.DurableCaches
import io.tpersson.ufw.durablejobs.*
import io.tpersson.ufw.examples.common.caches.AppCaches
import jakarta.inject.Inject

@DurableJobTypeDefinition(
    description = """
This job is responsible for refreshing a cache (`sensitive-data`) of sensitive data.
    """
)
public class SensitiveDataRefreshJob(
    override val id: DurableJobId = DurableJobId.new(),
) : DurableJob


public class SensitiveDataRefreshJobHandler @Inject constructor(
    private val caches: DurableCaches,
) : DurableJobHandler<SensitiveDataRefreshJob> {

    private val logger = createLogger()

    private val cache = caches.get(AppCaches.SensitiveDataCache)

    override suspend fun handle(job: SensitiveDataRefreshJob, context: DurableJobContext) {
        logger.info("Refreshing sensitive data")
        cache.put("some-sensitive-data", "not-really")
    }
}