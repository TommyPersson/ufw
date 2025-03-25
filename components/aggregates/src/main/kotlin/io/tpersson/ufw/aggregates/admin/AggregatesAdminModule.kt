package io.tpersson.ufw.aggregates.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.contracts.PaginatedListDTO
import io.tpersson.ufw.admin.contracts.toDTO
import io.tpersson.ufw.admin.utils.getPaginationOptions
import io.tpersson.ufw.aggregates.AggregateId
import io.tpersson.ufw.aggregates.internal.FactData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Instant
import java.util.*

@Singleton
public class AggregatesAdminModule @Inject constructor(
    private val adminFacade: AggregatesAdminFacade,
) : AdminModule {
    override val moduleId: String = "aggregates"

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/aggregates/aggregates/{aggregateId}/details") {
                val aggregateId = call.parameters.aggregateId!!

                val aggregateData = adminFacade.getAggregateDetails(aggregateId)
                if (aggregateData != null) {
                    call.respond(aggregateData.toDTO())
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/admin/api/aggregates/aggregates/{aggregateId}/facts") {
                val aggregateId = call.parameters.aggregateId!!
                val paginationOptions = call.getPaginationOptions()

                val facts = adminFacade.getAggregateFacts(aggregateId, paginationOptions).toDTO {
                    it.toDTO()
                }

                call.respond(facts)
            }
        }
    }
}

private val Parameters.aggregateId: AggregateId? get() = this["aggregateId"]?.let { AggregateId(it) }

public data class AggregateDetailsDTO(
    val id: String,
    val type: String,
    val version: Long,
    val json: String,
    val factTypes: List<FactType>
) {
    public data class FactType(
        val type: String,
    )
}

public data class AggregateFactDTO(
    val id: String,
    val aggregateId: String,
    val type: String,
    val json: String,
    val timestamp: Instant,
    val version: Long,
)

public fun FactData.toDTO(): AggregateFactDTO {
    return AggregateFactDTO(
        id = id.toString(),
        aggregateId = aggregateId,
        type = type,
        json = json,
        timestamp = timestamp,
        version = version,
    )
}

public fun AggregateData.toDTO(): AggregateDetailsDTO {
    return AggregateDetailsDTO(
        id = id,
        type = type,
        version = version,
        json = json,
        factTypes = factTypes.map {
            AggregateDetailsDTO.FactType(type = it.type)
        },
    )
}