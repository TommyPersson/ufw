package io.tpersson.ufw.aggregates.internal

import io.tpersson.ufw.aggregates.AbstractAggregate
import io.tpersson.ufw.aggregates.AggregateRepository

public interface AggregateRepositoryProvider {
    public fun add(repository: AggregateRepository<out AbstractAggregate<*>, *>)
    public fun getAll(): List<AggregateRepository<out AbstractAggregate<*>, *>>
}