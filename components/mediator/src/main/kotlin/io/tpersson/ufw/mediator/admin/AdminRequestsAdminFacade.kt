package io.tpersson.ufw.mediator.admin

import com.fasterxml.jackson.databind.JsonNode
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestDTO
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestExecutionResponseDTO

public interface AdminRequestsAdminFacade {
    public fun getRequests(requestType: RequestType): List<AdminRequestDTO>

    public suspend fun executeRequest(
        requestFqcn: String,
        requestBody: JsonNode,
        requestType: RequestType
    ): AdminRequestExecutionResponseDTO
}