package io.tpersson.ufw.aggregates.admin

import io.tpersson.ufw.aggregates.AggregateFactRepository
import io.tpersson.ufw.aggregates.AggregateId
import io.tpersson.ufw.aggregates.internal.FactData
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class AggregatesAdminFacadeImpl @Inject constructor(
    private val factRepository: AggregateFactRepository
) : AggregatesAdminFacade {
    override suspend fun getAggregateDetails(aggregateId: AggregateId) {
        TODO("Not yet implemented")
    }

    override suspend fun getAggregateFacts(
        aggregateId: AggregateId,
        paginationOptions: PaginationOptions
    ): PaginatedList<FactData> {
        return factRepository.getAllRaw(aggregateId, paginationOptions)
    }

}