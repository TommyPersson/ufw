package io.tpersson.ufw.aggregates.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.aggregates.*
import io.tpersson.ufw.aggregates.exceptions.AggregateVersionConflictException
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject
import jakarta.inject.Named
import org.postgresql.util.PSQLException
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

public class AggregateFactRepositoryImpl @Inject constructor(
    private val database: Database,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : AggregateFactRepository {

    override suspend fun insert(aggregateId: AggregateId, fact: Fact, version: Long, unitOfWork: UnitOfWork) {
        val factData = FactData(
            id = fact.id,
            aggregateId = aggregateId.value,
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
        val rawFacts = database.selectList(Queries.Selects.GetAll(aggregateId.value))

        return rawFacts.map { objectMapper.readValue(it.json, factClass.java) }
    }

    override suspend fun debugTruncate(unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.DebugTruncate)
    }

    internal data class FactData(
        val id: UUID,
        val aggregateId: String,
        val type: String,
        val json: String,
        val timestamp: Instant,
        val version: Long,
    )

    @Suppress("unused")
    private object Queries {
        private const val TableName = "ufw__aggregates__facts"

        object Selects {
            class GetAll(val aggregateId: String) : TypedSelect<FactData>(
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
                    type, 
                    json, 
                    timestamp, 
                    version
                ) VALUES (
                    :data.id,
                    :data.aggregateId,
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