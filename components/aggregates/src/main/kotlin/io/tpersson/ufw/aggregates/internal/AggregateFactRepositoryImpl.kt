package io.tpersson.ufw.aggregates.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.aggregates.AggregateFactRepository
import io.tpersson.ufw.aggregates.AggregateId
import io.tpersson.ufw.aggregates.Fact
import io.tpersson.ufw.aggregates.exceptions.AggregateVersionConflictException
import io.tpersson.ufw.aggregates.typeName
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.core.utils.paginate
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectList
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject
import jakarta.inject.Named
import kotlinx.coroutines.flow.toList
import org.postgresql.util.PSQLException
import kotlin.reflect.KClass

public class AggregateFactRepositoryImpl @Inject constructor(
    private val database: Database,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : AggregateFactRepository {

    override suspend fun insert(aggregateId: AggregateId, aggregateType: String, fact: Fact, version: Long, unitOfWork: UnitOfWork) {
        val factData = FactData(
            id = fact.id,
            aggregateId = aggregateId.value,
            aggregateType = aggregateType,
            type = fact.typeName,
            timestamp = fact.timestamp,
            json = objectMapper.writeValueAsString(fact),
            version = version
        )

        unitOfWork.add(Queries.Updates.Insert(factData), exceptionMapper = {
            if (it is PSQLException && it.message?.contains("ux_ufw__aggregates__facts_1") == true) {
                AggregateVersionConflictException(aggregateId, it)
            } else it
        })
    }

    override suspend fun <TFact : Fact> getAll(aggregateId: AggregateId, factClass: KClass<TFact>): List<TFact> {
        val rawFacts = paginate { // TODO expose pagination options and make caller do the pagination
            database.select(Queries.Selects.GetAll(aggregateId.value, it))
        }.toList().flatMap { it.items }

        return rawFacts.map { objectMapper.readValue(it.json, factClass.java) }
    }

    override suspend fun getAllRaw(
        aggregateId: AggregateId,
        paginationOptions: PaginationOptions,
    ): PaginatedList<FactData> {
        return database.select(Queries.Selects.GetAll(aggregateId.value, paginationOptions))
    }

    override suspend fun debugTruncate(unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.DebugTruncate)
    }

    @Suppress("unused")
    private object Queries {
        private const val TableName = "ufw__aggregates__facts"

        object Selects {
            class GetAll(
                val aggregateId: String,
                override val paginationOptions: PaginationOptions,
            ) : TypedSelectList<FactData>(
                """
                SELECT * 
                FROM $TableName
                WHERE aggregate_id = :aggregateId
                ORDER BY version
                """.trimIndent()
            )
        }

        object Updates {
            class Insert(val data: FactData) : TypedUpdate(
                """
                INSERT INTO $TableName (
                    id, 
                    aggregate_id, 
                    aggregate_type,
                    type, 
                    json, 
                    timestamp, 
                    version
                ) VALUES (
                    :data.id,
                    :data.aggregateId,
                    :data.aggregateType,
                    :data.type,
                    :data.json,
                    :data.timestamp,
                    :data.version
                )
                """.trimIndent()
            )

            object DebugTruncate : TypedUpdate("DELETE FROM $TableName", minimumAffectedRows = 0)
        }
    }
}


