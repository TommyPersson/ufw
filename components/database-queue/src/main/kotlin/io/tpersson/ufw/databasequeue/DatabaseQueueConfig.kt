package io.tpersson.ufw.databasequeue

import java.time.Duration

public data class DatabaseQueueConfig(
    val successfulItemExpirationDelay: Duration = Duration.ofDays(1),
    val failedItemExpirationDelay: Duration = Duration.ofDays(14),
)
