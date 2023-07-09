package io.tpersson.ufw.aggregates

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface AggregateRepository<TAggregate : AbstractAggregate<TFact>, TFact : Fact> {
    public suspend fun save(aggregate: TAggregate, unitOfWork: UnitOfWork)
    public suspend fun getById(id: AggregateId): TAggregate?
}