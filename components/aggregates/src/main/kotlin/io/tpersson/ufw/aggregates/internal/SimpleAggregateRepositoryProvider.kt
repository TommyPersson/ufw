package io.tpersson.ufw.aggregates.internal

import io.tpersson.ufw.aggregates.AggregateRepository
import java.util.*

public class SimpleAggregateRepositoryProvider : AggregateRepositoryProvider {
    private val repositories = Collections.synchronizedList(mutableListOf<AggregateRepository<*, *>>())

    public override fun add(repository: AggregateRepository<*, *>) {
        repositories.add(repository)
    }
    public override fun getAll(): List<AggregateRepository<*, *>> {
        return repositories.toList()
    }
}