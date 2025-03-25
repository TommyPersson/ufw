package io.tpersson.ufw.aggregates.admin

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.aggregates.AggregateFactRepository
import io.tpersson.ufw.aggregates.AggregateId
import io.tpersson.ufw.aggregates.internal.AggregateRepositoryProvider
import io.tpersson.ufw.aggregates.internal.FactData
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
public class AggregatesAdminFacadeImpl @Inject constructor(
    private val factRepository: AggregateFactRepository,
    private val repositoryProvider: AggregateRepositoryProvider,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : AggregatesAdminFacade {

    override suspend fun getAggregateDetails(aggregateId: AggregateId): AggregateData? {
        val aggregateType = factRepository.getAllRaw(aggregateId, PaginationOptions(limit = 1))
            .items
            .firstOrNull()
            ?.aggregateType
            ?: return null

        val repository = repositoryProvider.getAll()
            .firstOrNull { it.aggregateType == aggregateType }
            ?: return null

        val aggregate = repository.getById(aggregateId)

        val data = AggregateData(
            id = aggregateId.value,
            type = aggregateType,
            json = objectMapper.writeValueAsString(aggregate),
        )

        return data
    }

    override suspend fun getAggregateFacts(
        aggregateId: AggregateId,
        paginationOptions: PaginationOptions
    ): PaginatedList<FactData> {
        return factRepository.getAllRaw(aggregateId, paginationOptions)
    }

}

