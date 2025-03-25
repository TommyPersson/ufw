package io.tpersson.ufw.aggregates.admin

import io.tpersson.ufw.aggregates.AggregateId
import io.tpersson.ufw.aggregates.internal.FactData
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions

public interface AggregatesAdminFacade {
    public suspend fun getAggregateDetails(
        aggregateId: AggregateId
    ): AggregateData?

    public suspend fun getAggregateFacts(
        aggregateId: AggregateId,
        paginationOptions: PaginationOptions,
    ): PaginatedList<FactData>
}