package io.tpersson.ufw.aggregates

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import kotlin.reflect.KClass

public interface AggregateFactRepository {
    public suspend fun insert(aggregateId: AggregateId, fact: Fact, version: Long, unitOfWork: UnitOfWork)
    public suspend fun <TFact : Fact> getAll(aggregateId: AggregateId, factClass: KClass<TFact>): List<TFact>
}