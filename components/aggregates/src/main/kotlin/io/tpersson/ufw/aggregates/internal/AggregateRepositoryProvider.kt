package io.tpersson.ufw.aggregates.internal

import io.tpersson.ufw.aggregates.AggregateRepository

public interface AggregateRepositoryProvider {
    public fun add(repository: AggregateRepository<*, *>)
    public fun getAll(): List<AggregateRepository<*, *>>
}