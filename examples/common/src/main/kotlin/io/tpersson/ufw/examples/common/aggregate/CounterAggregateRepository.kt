package io.tpersson.ufw.examples.common.aggregate

import io.tpersson.ufw.aggregates.AbstractAggregateRepository
import io.tpersson.ufw.aggregates.AggregateId
import io.tpersson.ufw.aggregates.component.AggregatesComponent
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class CounterAggregateRepository @Inject constructor(
    component: AggregatesComponent,
) : AbstractAggregateRepository<CounterAggregate, CounterAggregate.Facts>(component) {

    override val aggregateType: String = "COUNTER"

    override suspend fun doSave(aggregate: CounterAggregate, version: Long, unitOfWork: UnitOfWork) {
    }

    override suspend fun doLoad(id: AggregateId, version: Long, facts: List<CounterAggregate.Facts>): CounterAggregate {
        return CounterAggregate(id, version, facts)
    }
}