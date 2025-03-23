package io.tpersson.ufw.examples.common.caches

import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import java.time.Duration

public object AppCaches {
    public val ExpensiveCalculations: DurableCacheDefinition<Long> = DurableCacheDefinition(
        id = "expensive-calculations",
        title = "Expensive Calculations",
        description = "Stores the results of expensive calculations.",
        valueType = Long::class,
        expiration = Duration.ofHours(1),
        inMemoryExpiration = Duration.ofSeconds(30),
    )
}