package io.tpersson.ufw.examples.common.caches

import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import java.time.Duration

public object AppCaches {
    public val ExpensiveCalculations: DurableCacheDefinition<Long> = object : DurableCacheDefinition<Long> {
        override val id = "expensive-calculations"
        override val title = "Expensive Calculations"
        override val description = """
            Stores the results of expensive calculations.
            
            But not too expensive, this is just a test after all. A test which also tests what happens if the 
            description text is longer than typical.
            
            What could possibly happen?
            """.trimIndent()
        override val valueType = Long::class
        override val containsSensitiveData = false
        override val expiration = Duration.ofHours(1)
        override val inMemoryExpiration = Duration.ofSeconds(30)
    }

    public val SensitiveDataCache: DurableCacheDefinition<String> = object : DurableCacheDefinition<String> {
        override val id = "sensitive-data"
        override val title = "Sensitive Data"
        override val description = "Stores cached sensitive data."
        override val valueType = String::class
        override val containsSensitiveData = true
        override val expiration = Duration.ofMinutes(10)
        override val inMemoryExpiration = Duration.ofSeconds(30)
    }
}