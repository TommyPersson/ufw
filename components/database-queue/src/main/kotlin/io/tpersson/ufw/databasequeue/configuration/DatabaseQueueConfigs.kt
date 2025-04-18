package io.tpersson.ufw.databasequeue.configuration

import io.tpersson.ufw.core.configuration.ConfigElement
import io.tpersson.ufw.core.configuration.Configs
import java.time.Duration

public object DatabaseQueueConfigs {
    public val MetricsMeasurementInterval: ConfigElement<Duration> = ConfigElement.of(
        "database-queue",
        "metrics-measurement-interval",
        default = Duration.ofSeconds(30)
    )

    public val SuccessfulItemExpirationDelay: ConfigElement<Duration> = ConfigElement.of(
        "database-queue",
        "successful-item-expiration-delay",
        default = Duration.ofDays(1)
    )

    public val FailedItemExpirationDelay: ConfigElement<Duration> = ConfigElement.of(
        "database-queue",
        "failed-item-expiration-delay",
        default = Duration.ofDays(14)
    )

    public val CancelledItemExpirationDelay: ConfigElement<Duration> = ConfigElement.of(
        "database-queue",
        "cancelled-item-expiration-delay",
        default = Duration.ofDays(1)
    )

    public val ItemExpirationInterval: ConfigElement<Duration> = ConfigElement.of(
        "database-queue",
        "item-expiration-interval",
        default = Duration.ofSeconds(30)
    )

    public val ItemReschedulingInterval: ConfigElement<Duration> = ConfigElement.of(
        "database-queue",
        "item-rescheduling-interval",
        default = Duration.ofSeconds(30)
    )

    public val WatchdogTimeout: ConfigElement<Duration> = ConfigElement.of(
        "database-queue",
        "watchdog-timeout",
        default = Duration.ofMinutes(10)
    )

    public val FallbackPollInterval: ConfigElement<Duration> = ConfigElement.of(
        "database-queue",
        "fallback-poll-interval",
        default = Duration.ofMillis(10)
    )
}

@Suppress("UnusedReceiverParameter")
public val Configs.DatabaseQueue: DatabaseQueueConfigs get() = DatabaseQueueConfigs
