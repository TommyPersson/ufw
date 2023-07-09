package io.tpersson.ufw.aggregates

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import kotlin.reflect.KClass

public abstract class AbstractAggregateRepository<TAggregate : AbstractAggregate<TFact>, TFact : Fact>(
    component: AggregatesComponent,
) : AggregateRepository<TAggregate, TFact> {

    private val factRepository = component.factRepository

    public override suspend fun save(aggregate: TAggregate, unitOfWork: UnitOfWork) {
        val newFacts = aggregate.pendingFacts.toList()
        if (newFacts.isEmpty()) {
            return
        }

        for ((i, fact) in newFacts.withIndex()) {
            factRepository.insert(aggregate.id, fact, aggregate.originalVersion + i + 1, unitOfWork)
        }

        val version = aggregate.originalVersion + newFacts.size

        doSave(aggregate, version, unitOfWork)

        aggregate.pendingFacts.clear()
    }

    public override suspend fun getById(id: AggregateId): TAggregate? {
        val facts = factRepository.getAll(id, factType)
        val version = facts.size.toLong()

        return doLoad(id, version, facts)
    }

    protected abstract suspend fun doSave(aggregate: TAggregate, version: Long, unitOfWork: UnitOfWork)

    protected abstract suspend fun doLoad(id: AggregateId, version: Long, facts: List<TFact>): TAggregate

    private val factType: KClass<TFact>
        get() = javaClass.kotlin
            .supertypes[0]
            .arguments[1]
            .type!!
            .classifier as KClass<TFact>
}