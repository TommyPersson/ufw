package io.tpersson.ufw.examples.common.caches

import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import java.time.Duration

public object AppCaches {
    public val ExpensiveCalculations: DurableCacheDefinition<Long> = DurableCacheDefinition(
        id = "expensive-calculations",
        title = "Expensive Calculations",
        description = """
            Stores the results of expensive calculations.
            
            But not too expensive, this is just a test after all. A test which also tests what happens if the 
            description text is longer than typical.
            
            What could possibly happen?
            """.trimIndent(),
        valueType = Long::class,
        containsSensitiveData = false,
        expiration = Duration.ofHours(1),
        inMemoryExpiration = Duration.ofSeconds(30),
    )

    public val SensitiveDataCache: DurableCacheDefinition<String> = DurableCacheDefinition(
        id = "sensitive-data",
        title = "Sensitive Data",
        description = "Stores cached sensitive data.",
        valueType = String::class,
        containsSensitiveData = true,
        expiration = Duration.ofMinutes(10),
        inMemoryExpiration = Duration.ofSeconds(30),
    )
}