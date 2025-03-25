package io.tpersson.ufw.aggregates

import io.tpersson.ufw.aggregates.internal.FactData
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import kotlin.reflect.KClass

public interface AggregateFactRepository {
    // TODO Typed AggregateId per repository?
    public suspend fun insert(
        aggregateId: AggregateId,
        aggregateType: String,
        fact: Fact,
        version: Long,
        unitOfWork: UnitOfWork
    )

    public suspend fun <TFact : Fact> getAll(
        aggregateId: AggregateId,
        factClass: KClass<TFact>
    ): List<TFact>

    public suspend fun getAllRaw(
        aggregateId: AggregateId,
        paginationOptions: PaginationOptions,
    ): PaginatedList<FactData>

    public suspend fun debugTruncate(unitOfWork: UnitOfWork)
}