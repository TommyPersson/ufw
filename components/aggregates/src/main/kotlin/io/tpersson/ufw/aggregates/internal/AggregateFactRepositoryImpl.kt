package io.tpersson.ufw.aggregates.internal

import io.tpersson.ufw.aggregates.AggregateFactRepository
import io.tpersson.ufw.aggregates.AggregateId
import io.tpersson.ufw.aggregates.Fact
import io.tpersson.ufw.aggregates.typeName
import io.tpersson.ufw.core.UFWObjectMapper
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject
import java.lang.Exception
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

public class AggregateFactRepositoryImpl @Inject constructor(
    private val database: Database,
    ufwObjectMapper: UFWObjectMapper,
) : AggregateFactRepository {

    private val objectMapper = ufwObjectMapper.objectMapper

    override suspend fun insert(aggregateId: AggregateId, fact: Fact, version: Long, unitOfWork: UnitOfWork) {
        val factData = FactData(
            id = UUID.randomUUID(),
            aggregateId = aggregateId.value,
            type = fact.typeName,
            timestamp = fact.timestamp,
            json = objectMapper.writeValueAsString(fact),
            version = version
        )

        // some kind of exception mapper to wrap "MinimumAffectedRows"-exception in "VersionConflict"-exception?
        unitOfWork.add(Queries.Updates.Insert(factData))
    }

    override suspend fun <TFact : Fact> getAll(aggregateId: AggregateId, factClass: KClass<TFact>): List<TFact> {
        val rawFacts = database.selectList(Queries.Selects.GetAll(aggregateId.value))

        return rawFacts.map { objectMapper.readValue(it.json, factClass.java) }
    }

    internal data class FactData(
        val id: UUID,
        val aggregateId: String,
        val type: String,
        val json: String,
        val timestamp: Instant,
        val version: Long,
    )

    private object Queries {
        object Selects {
            class GetAll(val aggregateId: String) : TypedSelect<FactData>(
                """
                SELECT * 
                FROM ufw__aggregates__facts
                WHERE aggregate_id = :aggregateId
                ORDER BY version
                """.trimIndent()
            )
        }

        object Updates {
            class Insert(val data: FactData) : TypedUpdate(
                """
                INSERT INTO ufw__aggregates__facts (
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
        }
    }
}