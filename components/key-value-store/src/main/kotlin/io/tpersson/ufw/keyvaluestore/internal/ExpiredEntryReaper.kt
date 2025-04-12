package io.tpersson.ufw.keyvaluestore.internal

import io.tpersson.ufw.keyvaluestore.KeyValueStoreConfig
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Clock

public class ExpiredEntryReaper @Inject constructor(
    private val storageEngine: StorageEngine,
    private val clock: Clock,
    private val config: KeyValueStoreConfig,
) : ManagedPeriodicTask(
    interval = config.expiredEntryReapingInterval
) {
    override suspend fun runOnce() {
        val numDeleted = storageEngine.deleteExpiredEntries(clock.instant())
        if (numDeleted > 0) {
            logger.info("Deleted $numDeleted expired entries.")
        }
    }
}