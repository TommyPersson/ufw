package io.tpersson.ufw.aggregates.admin

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.aggregates.AbstractAggregate
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Singleton
public class AggregatesAdminFacadeImpl @Inject constructor(
    private val factRepository: AggregateFactRepository,
    private val repositoryProvider: AggregateRepositoryProvider,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : AggregatesAdminFacade {

    private val factTypesCache = ConcurrentHashMap<KClass<*>, List<AggregateData.FactType>>()

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
            ?: return null

        val factTypes = getAvailableFactTypesForAggregate(aggregate)

        val data = AggregateData(
            id = aggregateId.value,
            type = aggregateType,
            json = objectMapper.writeValueAsString(aggregate),
            factTypes = factTypes
        )

        return data
    }

    override suspend fun getAggregateFacts(
        aggregateId: AggregateId,
        paginationOptions: PaginationOptions
    ): PaginatedList<FactData> {
        return factRepository.getAllRaw(aggregateId, paginationOptions)
    }

    private fun getAvailableFactTypesForAggregate(aggregate: AbstractAggregate<*>): List<AggregateData.FactType> {
        return factTypesCache.getOrPut(aggregate::class) {
            val aggregateFactBaseClass = aggregate::class.supertypes
                .first { it.classifier == AbstractAggregate::class }
                .arguments
                .single()
                .type!!
                .classifier.let { it as KClass<*> }

            aggregateFactBaseClass
                .sealedSubclasses
                .mapNotNull { it.findAnnotation<JsonTypeName>() }
                .map { AggregateData.FactType(type = it.value) }
        }
    }
}

