package io.tpersson.ufw.databasequeue

import java.time.Duration

public data class DatabaseQueueConfig(
    val successfulItemExpirationDelay: Duration = Duration.ofDays(1),
    val failedItemExpirationDelay: Duration = Duration.ofDays(14),
    val expirationInterval: Duration = Duration.ofSeconds(30),
    val watchdogTimeout: Duration = Duration.ofMinutes(10),
)
