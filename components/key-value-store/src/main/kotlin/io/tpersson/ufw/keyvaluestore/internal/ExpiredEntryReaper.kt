package io.tpersson.ufw.keyvaluestore.internal

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.keyvaluestore.KeyValueStoreConfig
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.Clock

public class ExpiredEntryReaper @Inject constructor(
    private val storageEngine: StorageEngine,
    private val clock: Clock,
    private val config: KeyValueStoreConfig,
) : ManagedJob() {

    private val interval = config.expiredEntryReapingInterval

    override suspend fun launch() {
        forever(logger) {
            delay(interval.toMillis())

            val numDeleted = storageEngine.deleteExpiredEntries(clock.instant())
            if (numDeleted > 0) {
                logger.info("Deleted $numDeleted expired entries.")
            }
        }
    }
}